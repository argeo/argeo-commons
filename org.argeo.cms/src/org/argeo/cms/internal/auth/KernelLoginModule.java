package org.argeo.cms.internal.auth;

public class KernelLoginModule {//implements LoginModule {
//	private Subject subject;
//
//	@Override
//	public void initialize(Subject subject, CallbackHandler callbackHandler,
//			Map<String, ?> sharedState, Map<String, ?> options) {
//		this.subject = subject;
//	}
//
//	@Override
//	public boolean login() throws LoginException {
//		// TODO check permission at code level ?
//		return true;
//	}
//
//	@Override
//	public boolean commit() throws LoginException {
//		// Check that kernel has been logged in w/ certificate
//		// Name
//		Set<X500Principal> names = subject.getPrincipals(X500Principal.class);
//		if (names.isEmpty() || names.size() > 1) {
//			// throw new LoginException("Kernel must have been named");
//			// TODO set not hardened
//			subject.getPrincipals().add(
//					new X500Principal(AuthConstants.ROLE_KERNEL));
//		} else {
//			X500Principal name = names.iterator().next();
//			if (!AuthConstants.ROLE_KERNEL.equals(name.getName()))
//				throw new LoginException("Kernel must be named "
//						+ AuthConstants.ROLE_KERNEL);
//			// Private certificate
//			Set<X500PrivateCredential> privateCerts = subject
//					.getPrivateCredentials(X500PrivateCredential.class);
//			X500PrivateCredential privateCert = null;
//			for (X500PrivateCredential pCert : privateCerts) {
//				if (pCert.getCertificate().getSubjectX500Principal()
//						.equals(name)) {
//					privateCert = pCert;
//				}
//			}
//			if (privateCert == null)
//				throw new LoginException(
//						"Kernel must have a private certificate");
//			// Certificate path
//			Set<CertPath> certPaths = subject
//					.getPublicCredentials(CertPath.class);
//			CertPath certPath = null;
//			for (CertPath cPath : certPaths) {
//				if (cPath.getCertificates().get(0)
//						.equals(privateCert.getCertificate())) {
//					certPath = cPath;
//				}
//			}
//			if (certPath == null)
//				throw new LoginException("Kernel must have a certificate path");
//		}
//		Set<Principal> principals = subject.getPrincipals();
//		// Add admin roles
//
//		// Add data access roles
//		principals.add(new AdminPrincipal(SecurityConstants.ADMIN_ID));
//
//		return true;
//	}
//
//	@Override
//	public boolean abort() throws LoginException {
//		return true;
//	}
//
//	@Override
//	public boolean logout() throws LoginException {
//		// clear everything
//		subject.getPrincipals().clear();
//		subject.getPublicCredentials().clear();
//		subject.getPrivateCredentials().clear();
//		return true;
//	}

}
