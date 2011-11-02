package org.argeo.security.jcr;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionManager;

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.OsAuthenticationToken;
import org.argeo.security.core.OsAuthenticationProvider;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;

/** Relies on OS to authenticate and additionaly setup JCR */
public class OsJcrAuthenticationProvider extends OsAuthenticationProvider {
	private Repository repository;
	private String securityWorkspace = "security";
	private Session securitySession;

	public void init() {
		try {
			securitySession = repository.login(securityWorkspace);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot initialize", e);
		}
	}

	public void destroy() {
		JcrUtils.logoutQuietly(securitySession);
	}

	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		final OsAuthenticationToken authen = (OsAuthenticationToken) super
				.authenticate(authentication);
		try {
			// WARNING: at this stage we assume that the java properties
			// will have the same value
			String username = System.getProperty("user.name");
			Node userHome = JcrUtils.createUserHomeIfNeeded(securitySession,
					username);
			Node userProfile = userHome.hasNode(ArgeoNames.ARGEO_PROFILE) ? userHome
					.getNode(ArgeoNames.ARGEO_PROFILE) : JcrUtils
					.createUserProfile(securitySession, username);
			if (securitySession.hasPendingChanges())
				securitySession.save();
			VersionManager versionManager = securitySession.getWorkspace()
					.getVersionManager();
			if (versionManager.isCheckedOut(userProfile.getPath()))
				versionManager.checkin(userProfile.getPath());

			JcrUserDetails.checkAccountStatus(userProfile);
			// user details
			JcrUserDetails userDetails = new JcrUserDetails(userProfile, authen
					.getCredentials().toString(), getBaseAuthorities());
			authen.setDetails(userDetails);
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(securitySession);
			throw new ArgeoException(
					"Unexpected exception when synchronizing OS and JCR security ",
					e);
		} finally {
			JcrUtils.logoutQuietly(securitySession);
		}
		return authen;
	}

	public void setSecurityWorkspace(String securityWorkspace) {
		this.securityWorkspace = securityWorkspace;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}
}
