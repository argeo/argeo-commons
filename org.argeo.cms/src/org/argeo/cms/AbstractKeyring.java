package org.argeo.cms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
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

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.keyring.PBEKeySpecCallback;
import org.argeo.cms.util.CurrentSubject;
import org.argeo.cms.util.StreamUtils;
import org.argeo.api.cms.keyring.CryptoKeyring;
import org.argeo.api.cms.keyring.Keyring;

/** username / password based keyring. TODO internationalize */
public abstract class AbstractKeyring implements Keyring, CryptoKeyring {
	// public final static String DEFAULT_KEYRING_LOGIN_CONTEXT = "KEYRING";

	// private String loginContextName = DEFAULT_KEYRING_LOGIN_CONTEXT;
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
	protected SecretKey getSecretKey(char[] password) {
		Subject subject = CurrentSubject.current();
		if (subject == null)
			throw new IllegalStateException("Current subject cannot be null");
		// we assume only one secrete key is available
		Iterator<SecretKey> iterator = subject.getPrivateCredentials(SecretKey.class).iterator();
		if (!iterator.hasNext() || password != null) {// not initialized
			CallbackHandler callbackHandler = password == null ? new KeyringCallbackHandler()
					: new PasswordProvidedCallBackHandler(password);
			ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			try {
				LoginContext loginContext = new LoginContext(CmsAuth.LOGIN_CONTEXT_KEYRING, subject, callbackHandler);
				loginContext.login();
				// FIXME will login even if password is wrong
				iterator = subject.getPrivateCredentials(SecretKey.class).iterator();
				return iterator.next();
			} catch (LoginException e) {
				throw new IllegalStateException("Keyring login failed", e);
			} finally {
				Thread.currentThread().setContextClassLoader(currentContextClassLoader);
			}

		} else {
			SecretKey secretKey = iterator.next();
			if (iterator.hasNext())
				throw new IllegalStateException("More than one secret key in private credentials");
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
		// InputStream in = getAsStream(path);
		// CharArrayWriter writer = null;
		// Reader reader = null;
		try (InputStream in = getAsStream(path);
				CharArrayWriter writer = new CharArrayWriter();
				Reader reader = new InputStreamReader(in, charset);) {
			StreamUtils.copy(reader, writer);
			return writer.toCharArray();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot decrypt to char array", e);
		} finally {
			// IOUtils.closeQuietly(reader);
			// IOUtils.closeQuietly(in);
			// IOUtils.closeQuietly(writer);
		}
	}

	public void set(String path, char[] arr) {
		// ByteArrayOutputStream out = new ByteArrayOutputStream();
		// ByteArrayInputStream in = null;
		// Writer writer = null;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				Writer writer = new OutputStreamWriter(out, charset);) {
			// writer = new OutputStreamWriter(out, charset);
			writer.write(arr);
			writer.flush();
			// in = new ByteArrayInputStream(out.toByteArray());
			try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());) {
				set(path, in);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot encrypt to char array", e);
		} finally {
			// IOUtils.closeQuietly(writer);
			// IOUtils.closeQuietly(out);
			// IOUtils.closeQuietly(in);
		}
	}

	public void unlock(char[] password) {
		if (!isSetup())
			setup(password);
		SecretKey secretKey = getSecretKey(password);
		if (secretKey == null)
			throw new IllegalStateException("Could not unlock keyring");
	}

	protected Provider getSecurityProvider() {
		return Security.getProvider(securityProviderName);
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

	// @Deprecated
	// protected static byte[] hash(char[] password, byte[] salt, Integer
	// iterationCount) {
	// ByteArrayOutputStream out = null;
	// OutputStreamWriter writer = null;
	// try {
	// out = new ByteArrayOutputStream();
	// writer = new OutputStreamWriter(out, "UTF-8");
	// writer.write(password);
	// MessageDigest pwDigest = MessageDigest.getInstance("SHA-256");
	// pwDigest.reset();
	// pwDigest.update(salt);
	// byte[] btPass = pwDigest.digest(out.toByteArray());
	// for (int i = 0; i < iterationCount; i++) {
	// pwDigest.reset();
	// btPass = pwDigest.digest(btPass);
	// }
	// return btPass;
	// } catch (Exception e) {
	// throw new CmsException("Cannot hash", e);
	// } finally {
	// IOUtils.closeQuietly(out);
	// IOUtils.closeQuietly(writer);
	// }
	//
	// }

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
			throw new IllegalStateException("Cannot ask for a password", e);
		}

	}

	class KeyringCallbackHandler implements CallbackHandler {
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			// checks
			if (callbacks.length != 2)
				throw new IllegalArgumentException(
						"Keyring requires 2 and only 2 callbacks: {PasswordCallback,PBEKeySpecCallback}");
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
				TextOutputCallback textCb1 = new TextOutputCallback(TextOutputCallback.INFORMATION,
						"Enter a master password which will protect your private data");
				TextOutputCallback textCb2 = new TextOutputCallback(TextOutputCallback.INFORMATION,
						"(for example your credentials to third-party services)");
				TextOutputCallback textCb3 = new TextOutputCallback(TextOutputCallback.INFORMATION,
						"Don't forget this password since the data cannot be read without it");
				PasswordCallback confirmPasswordCb = new PasswordCallback("Confirm password", false);
				// first try
				Callback[] dialogCbs = new Callback[] { textCb1, textCb2, textCb3, passwordCb, confirmPasswordCb };
				defaultCallbackHandler.handle(dialogCbs);

				// if passwords different, retry (except if cancelled)
				while (passwordCb.getPassword() != null
						&& !Arrays.equals(passwordCb.getPassword(), confirmPasswordCb.getPassword())) {
					TextOutputCallback textCb = new TextOutputCallback(TextOutputCallback.ERROR,
							"The passwords do not match");
					dialogCbs = new Callback[] { textCb, passwordCb, confirmPasswordCb };
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

	class PasswordProvidedCallBackHandler implements CallbackHandler {
		private final char[] password;

		public PasswordProvidedCallBackHandler(char[] password) {
			this.password = password;
		}

		@Override
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			// checks
			if (callbacks.length != 2)
				throw new IllegalArgumentException(
						"Keyring requires 2 and only 2 callbacks: {PasswordCallback,PBEKeySpecCallback}");
			if (!(callbacks[0] instanceof PasswordCallback))
				throw new UnsupportedCallbackException(callbacks[0]);
			if (!(callbacks[1] instanceof PBEKeySpecCallback))
				throw new UnsupportedCallbackException(callbacks[0]);

			PasswordCallback passwordCb = (PasswordCallback) callbacks[0];
			passwordCb.setPassword(password);
			PBEKeySpecCallback pbeCb = (PBEKeySpecCallback) callbacks[1];
			handleKeySpecCallback(pbeCb);
		}

	}
}
