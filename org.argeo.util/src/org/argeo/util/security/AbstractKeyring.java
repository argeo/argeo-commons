/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.util.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Iterator;

import javax.crypto.SecretKey;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.util.internal.UtilsException;
import org.argeo.util.internal.StreamUtils;

/** username / password based keyring. TODO internationalize */
public abstract class AbstractKeyring implements Keyring, CryptoKeyring {
	public final static String DEFAULT_KEYRING_LOGIN_CONTEXT = "KEYRING";

	private String loginContextName = DEFAULT_KEYRING_LOGIN_CONTEXT;
	private CallbackHandler defaultCallbackHandler;

	private String charset = "UTF-8";

	/**
	 * Default provider is bouncy castle, in order to have consistent behaviour
	 * across implementations
	 */
	private String securityProviderName = "BC";

	/**
	 * Whether the keyring has already been created in the past with a master
	 * password
	 */
	protected abstract Boolean isSetup();

	/**
	 * Setup the keyring persistently, {@link #isSetup()} must return true
	 * afterwards
	 */
	protected abstract void setup(char[] password);

	/** Populates the key spec callback */
	protected abstract void handleKeySpecCallback(PBEKeySpecCallback pbeCallback);

	protected abstract void encrypt(String path, InputStream unencrypted);

	protected abstract InputStream decrypt(String path);

	/** Triggers lazy initialization */
	protected SecretKey getSecretKey() {
		Subject subject = Subject.getSubject(AccessController.getContext());
		// we assume only one secrete key is available
		Iterator<SecretKey> iterator = subject.getPrivateCredentials(
				SecretKey.class).iterator();
		if (!iterator.hasNext()) {// not initialized
			CallbackHandler callbackHandler = new KeyringCallbackHandler();
			try {
				LoginContext loginContext = new LoginContext(loginContextName,
						subject, callbackHandler);
				loginContext.login();
				// FIXME will login even if password is wrong
				iterator = subject.getPrivateCredentials(SecretKey.class)
						.iterator();
				return iterator.next();
			} catch (LoginException e) {
				throw new UtilsException("Keyring login failed", e);
			}

		} else {
			SecretKey secretKey = iterator.next();
			if (iterator.hasNext())
				throw new UtilsException(
						"More than one secret key in private credentials");
			return secretKey;
		}
	}

	public InputStream getAsStream(String path) {
		return decrypt(path);
	}

	public void set(String path, InputStream in) {
		encrypt(path, in);
	}

	public char[] getAsChars(String path) {
		InputStream in = getAsStream(path);
		CharArrayWriter writer = null;
		Reader reader = null;
		try {
			writer = new CharArrayWriter();
			reader = new InputStreamReader(in, charset);
			StreamUtils.copy(reader, writer);
			return writer.toCharArray();
		} catch (IOException e) {
			throw new UtilsException("Cannot decrypt to char array", e);
		} finally {
			StreamUtils.closeQuietly(reader);
			StreamUtils.closeQuietly(in);
			StreamUtils.closeQuietly(writer);
		}
	}

	public void set(String path, char[] arr) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayInputStream in = null;
		Writer writer = null;
		try {
			writer = new OutputStreamWriter(out, charset);
			writer.write(arr);
			writer.flush();
			in = new ByteArrayInputStream(out.toByteArray());
			set(path, in);
		} catch (IOException e) {
			throw new UtilsException("Cannot encrypt to char array", e);
		} finally {
			StreamUtils.closeQuietly(writer);
			StreamUtils.closeQuietly(out);
			StreamUtils.closeQuietly(in);
		}
	}

	protected Provider getSecurityProvider() {
		return Security.getProvider(securityProviderName);
	}

	public void setLoginContextName(String loginContextName) {
		this.loginContextName = loginContextName;
	}

	public void setDefaultCallbackHandler(CallbackHandler defaultCallbackHandler) {
		this.defaultCallbackHandler = defaultCallbackHandler;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setSecurityProviderName(String securityProviderName) {
		this.securityProviderName = securityProviderName;
	}

	@Deprecated
	protected static byte[] hash(char[] password, byte[] salt,
			Integer iterationCount) {
		ByteArrayOutputStream out = null;
		OutputStreamWriter writer = null;
		try {
			out = new ByteArrayOutputStream();
			writer = new OutputStreamWriter(out, "UTF-8");
			writer.write(password);
			MessageDigest pwDigest = MessageDigest.getInstance("SHA-256");
			pwDigest.reset();
			pwDigest.update(salt);
			byte[] btPass = pwDigest.digest(out.toByteArray());
			for (int i = 0; i < iterationCount; i++) {
				pwDigest.reset();
				btPass = pwDigest.digest(btPass);
			}
			return btPass;
		} catch (Exception e) {
			throw new UtilsException("Cannot hash", e);
		} finally {
			StreamUtils.closeQuietly(out);
			StreamUtils.closeQuietly(writer);
		}

	}

	/**
	 * Convenience method using the underlying callback to ask for a password
	 * (typically used when the password is not saved in the keyring)
	 */
	protected char[] ask() {
		PasswordCallback passwordCb = new PasswordCallback("Password", false);
		Callback[] dialogCbs = new Callback[] { passwordCb };
		try {
			defaultCallbackHandler.handle(dialogCbs);
			char[] password = passwordCb.getPassword();
			return password;
		} catch (Exception e) {
			throw new UtilsException("Cannot ask for a password", e);
		}

	}

	class KeyringCallbackHandler implements CallbackHandler {
		public void handle(Callback[] callbacks) throws IOException,
				UnsupportedCallbackException {
			// checks
			if (callbacks.length != 2)
				throw new IllegalArgumentException(
						"Keyring required 2 and only 2 callbacks: {PasswordCallback,PBEKeySpecCallback}");
			if (!(callbacks[0] instanceof PasswordCallback))
				throw new UnsupportedCallbackException(callbacks[0]);
			if (!(callbacks[1] instanceof PBEKeySpecCallback))
				throw new UnsupportedCallbackException(callbacks[0]);

			PasswordCallback passwordCb = (PasswordCallback) callbacks[0];
			PBEKeySpecCallback pbeCb = (PBEKeySpecCallback) callbacks[1];

			if (isSetup()) {
				Callback[] dialogCbs = new Callback[] { passwordCb };
				defaultCallbackHandler.handle(dialogCbs);
			} else {// setup keyring
				TextOutputCallback textCb1 = new TextOutputCallback(
						TextOutputCallback.INFORMATION,
						"Enter a master password which will protect your private data");
				TextOutputCallback textCb2 = new TextOutputCallback(
						TextOutputCallback.INFORMATION,
						"(for example your credentials to third-party services)");
				TextOutputCallback textCb3 = new TextOutputCallback(
						TextOutputCallback.INFORMATION,
						"Don't forget this password since the data cannot be read without it");
				PasswordCallback confirmPasswordCb = new PasswordCallback(
						"Confirm password", false);
				// first try
				Callback[] dialogCbs = new Callback[] { textCb1, textCb2,
						textCb3, passwordCb, confirmPasswordCb };
				defaultCallbackHandler.handle(dialogCbs);

				// if passwords different, retry (except if cancelled)
				while (passwordCb.getPassword() != null
						&& !Arrays.equals(passwordCb.getPassword(),
								confirmPasswordCb.getPassword())) {
					TextOutputCallback textCb = new TextOutputCallback(
							TextOutputCallback.ERROR,
							"The passwords do not match");
					dialogCbs = new Callback[] { textCb, passwordCb,
							confirmPasswordCb };
					defaultCallbackHandler.handle(dialogCbs);
				}

				if (passwordCb.getPassword() != null) {// not cancelled
					setup(passwordCb.getPassword());
				}
			}

			if (passwordCb.getPassword() != null)
				handleKeySpecCallback(pbeCb);
		}

	}
}
