package org.argeo.cms.auth;

import java.security.AccessController;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.argeo.api.security.PBEKeySpecCallback;
import org.argeo.util.PasswordEncryption;

/** Adds a secret key to the private credentials */
public class KeyringLoginModule implements LoginModule {
	private Subject subject;
	private CallbackHandler callbackHandler;
	private SecretKey secretKey;

	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		if (subject == null) {
			subject = Subject.getSubject(AccessController.getContext());
		}
		this.callbackHandler = callbackHandler;
	}

	public boolean login() throws LoginException {
//		Set<SecretKey> pbes = subject.getPrivateCredentials(SecretKey.class);
//		if (pbes.size() > 0)
//			return true;
		PasswordCallback pc = new PasswordCallback("Master password", false);
		PBEKeySpecCallback pbeCb = new PBEKeySpecCallback();
		Callback[] callbacks = { pc, pbeCb };
		try {
			callbackHandler.handle(callbacks);
			char[] password = pc.getPassword();

			SecretKeyFactory keyFac = SecretKeyFactory.getInstance(pbeCb.getSecretKeyFactory());
			PBEKeySpec keySpec;
			if (pbeCb.getKeyLength() != null)
				keySpec = new PBEKeySpec(password, pbeCb.getSalt(), pbeCb.getIterationCount(), pbeCb.getKeyLength());
			else
				keySpec = new PBEKeySpec(password, pbeCb.getSalt(), pbeCb.getIterationCount());

			String secKeyEncryption = pbeCb.getSecretKeyEncryption();
			if (secKeyEncryption != null) {
				SecretKey tmp = keyFac.generateSecret(keySpec);
				secretKey = new SecretKeySpec(tmp.getEncoded(), secKeyEncryption);
			} else {
				secretKey = keyFac.generateSecret(keySpec);
			}
		} catch (Exception e) {
			LoginException le = new LoginException("Cannot login keyring");
			le.initCause(e);
			throw le;
		}
		return true;
	}

	public boolean commit() throws LoginException {
		if (secretKey != null) {
			subject.getPrivateCredentials().removeAll(subject.getPrivateCredentials(SecretKey.class));
			subject.getPrivateCredentials().add(secretKey);
		}
		return true;
	}

	public boolean abort() throws LoginException {
		return true;
	}

	public boolean logout() throws LoginException {
		Set<PasswordEncryption> pbes = subject.getPrivateCredentials(PasswordEncryption.class);
		pbes.clear();
		return true;
	}

}
