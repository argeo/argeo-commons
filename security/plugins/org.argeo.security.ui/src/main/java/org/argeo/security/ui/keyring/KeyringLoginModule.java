package org.argeo.security.ui.keyring;

import java.security.AccessController;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.logging.LogConfigurationException;
import org.argeo.util.crypto.PasswordBasedEncryption;

public class KeyringLoginModule implements LoginModule {
	private Subject subject;
	private CallbackHandler callbackHandler;
	private PasswordBasedEncryption passwordBasedEncryption;

	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		this.subject = subject;
		if (subject == null) {
			subject = Subject.getSubject(AccessController.getContext());
		}
		this.callbackHandler = callbackHandler;
	}

	public boolean login() throws LoginException {
		Set<PasswordBasedEncryption> pbes = subject
				.getPrivateCredentials(PasswordBasedEncryption.class);
		if (pbes.size() > 0)
			return true;
		PasswordCallback pc = new PasswordCallback("Master password", false);
		Callback[] callbacks = { pc };
		try {
			callbackHandler.handle(callbacks);
			passwordBasedEncryption = new PasswordBasedEncryption(
					pc.getPassword());
		} catch (Exception e) {
			throw new LogConfigurationException(e);
		}
		return true;
	}

	public boolean commit() throws LoginException {
		if (passwordBasedEncryption != null)
			subject.getPrivateCredentials(PasswordBasedEncryption.class).add(
					passwordBasedEncryption);
		return true;
	}

	public boolean abort() throws LoginException {
		return true;
	}

	public boolean logout() throws LoginException {
		Set<PasswordBasedEncryption> pbes = subject
				.getPrivateCredentials(PasswordBasedEncryption.class);
		pbes.clear();
		return true;
	}

}
