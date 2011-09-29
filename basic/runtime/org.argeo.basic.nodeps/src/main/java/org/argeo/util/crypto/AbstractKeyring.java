package org.argeo.util.crypto;

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

import org.argeo.ArgeoException;
import org.argeo.StreamUtils;

/** username / password based keyring. TODO internationalize */
public abstract class AbstractKeyring implements Keyring {
	public final static String DEFAULT_KEYRING_LOGIN_CONTEXT = "KEYRING";

	private String loginContextName = DEFAULT_KEYRING_LOGIN_CONTEXT;
	private CallbackHandler defaultCallbackHandler;

	private String charset = "UTF-8";

	/**
	 * Whether the keyring has already been created in the past with a master
	 * password
	 */
	protected abstract Boolean isSetup();

	/**
	 * Setup the keyring persistently, {@link #isSetup()} must return true
	 * afterwards
	 */
	protected abstract void setup();

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
				throw new ArgeoException("Keyring login failed", e);
			}

		} else {
			SecretKey secretKey = iterator.next();
			if (iterator.hasNext())
				throw new ArgeoException(
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
			throw new ArgeoException("Cannot decrypt to char array", e);
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
			in = new ByteArrayInputStream(out.toByteArray());
			set(path, in);
		} catch (IOException e) {
			throw new ArgeoException("Cannot encrypt to char array", e);
		} finally {
			StreamUtils.closeQuietly(writer);
			StreamUtils.closeQuietly(out);
			StreamUtils.closeQuietly(in);
		}
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
						"Enter a master password");
				TextOutputCallback textCb2 = new TextOutputCallback(
						TextOutputCallback.INFORMATION,
						"It will encrypt your private data");
				TextOutputCallback textCb3 = new TextOutputCallback(
						TextOutputCallback.INFORMATION,
						"Don't forget it or your data is lost");
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

				if (passwordCb.getPassword() != null)// not cancelled
					setup();
			}

			if (passwordCb.getPassword() != null)
				handleKeySpecCallback(pbeCb);
		}

	}
}
