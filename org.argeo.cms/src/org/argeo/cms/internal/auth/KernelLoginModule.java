package org.argeo.cms.internal.auth;

import java.security.Principal;
import java.security.cert.CertPath;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;
import javax.security.auth.x500.X500PrivateCredential;

import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.argeo.cms.auth.AuthConstants;

public class KernelLoginModule implements LoginModule {
	private Subject subject;

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		this.subject = subject;
	}

	@Override
	public boolean login() throws LoginException {
		// TODO check permission at code level ?
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		// Check that kernel has been logged in w/ certificate
		// Name
		Set<X500Principal> names = subject.getPrincipals(X500Principal.class);
		if (names.isEmpty() || names.size() > 1)
			throw new LoginException("Kernel must have been named");
		X500Principal name = names.iterator().next();
		if (!AuthConstants.ROLE_KERNEL.equals(name.getName()))
			throw new LoginException("Kernel must be named named "
					+ AuthConstants.ROLE_KERNEL);
		// Private certificate
		Set<X500PrivateCredential> privateCerts = subject
				.getPrivateCredentials(X500PrivateCredential.class);
		X500PrivateCredential privateCert = null;
		for (X500PrivateCredential pCert : privateCerts) {
			if (pCert.getCertificate().getSubjectX500Principal().equals(name)) {
				privateCert = pCert;
			}
		}
		if (privateCert == null)
			throw new LoginException("Kernel must have a private certificate");
		// Certificate path
		Set<CertPath> certPaths = subject.getPublicCredentials(CertPath.class);
		CertPath certPath = null;
		for (CertPath cPath : certPaths) {
			if (cPath.getCertificates().get(0)
					.equals(privateCert.getCertificate())) {
				certPath = cPath;
			}
		}
		if (certPath == null)
			throw new LoginException("Kernel must have a certificate path");

		Set<Principal> principals = subject.getPrincipals();
		// Add admin roles

		// Add data access roles
		principals.add(new AdminPrincipal(SecurityConstants.ADMIN_ID));

		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		// clear everything
		subject.getPrincipals().clear();
		subject.getPublicCredentials().clear();
		subject.getPrivateCredentials().clear();
		return true;
	}

}
