package org.argeo.security.ldap.jcr;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.jcr.JcrUserDetails;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.ldap.UserDetailsContextMapper;

/**
 * Maps LDAP attributes and JCR properties. This class is meant to be robust,
 * checks of which values should be mandatory should be performed at a higher
 * level.
 */
public class JcrUserDetailsContextMapper implements UserDetailsContextMapper,
		ArgeoNames {
	private final static Log log = LogFactory
			.getLog(JcrUserDetailsContextMapper.class);

	private String usernameAttribute;
	private String passwordAttribute;
	private String homeBasePath;
	private String[] userClasses;

	private Map<String, String> propertyToAttributes = new HashMap<String, String>();
	private Executor systemExecutor;
	private RepositoryFactory repositoryFactory;

	public UserDetails mapUserFromContext(final DirContextOperations ctx,
			final String username, GrantedAuthority[] authorities) {
		if (repositoryFactory == null)
			throw new ArgeoException("No JCR repository factory registered");
		final StringBuffer userHomePathT = new StringBuffer("");
		systemExecutor.execute(new Runnable() {
			public void run() {
				String userHomepath = mapLdapToJcr(username, ctx);
				userHomePathT.append(userHomepath);
			}
		});

		// password
		byte[] arr = (byte[]) ctx
				.getAttributeSortedStringSet(passwordAttribute).first();
		JcrUserDetails userDetails = new JcrUserDetails(
				userHomePathT.toString(), username, new String(arr), true,
				true, true, true, authorities);
		// erase password
		Arrays.fill(arr, (byte) 0);
		return userDetails;
	}

	/** @return path to the user home node */
	protected String mapLdapToJcr(String username, DirContextOperations ctx) {
		Session session = null;
		try {
			Repository nodeRepo = JcrUtils.getRepositoryByAlias(
					repositoryFactory, ArgeoJcrConstants.ALIAS_NODE);
			session = nodeRepo.login();
			Node userHome = JcrUtils.getUserHome(session, username);
			if (userHome == null)
				userHome = createUserHome(session, username);
			String userHomePath = userHome.getPath();
			Node userProfile = userHome.hasNode(ARGEO_USER_PROFILE) ? userHome
					.getNode(ARGEO_USER_PROFILE) : userHome
					.addNode(ARGEO_USER_PROFILE);
			for (String jcrProperty : propertyToAttributes.keySet())
				ldapToJcr(userProfile, jcrProperty, ctx);
			session.save();
			if (log.isDebugEnabled())
				log.debug("Mapped " + ctx.getDn() + " to " + userProfile);
			return userHomePath;
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoException("Cannot synchronize JCR and LDAP", e);
		} finally {
			session.logout();
		}
	}

	protected Node createUserHome(Session session, String username) {
		try {
			Node userHome = JcrUtils.mkdirs(session,
					usernameToHomePath(username));
			userHome.addMixin(ArgeoTypes.ARGEO_USER_HOME);
			userHome.setProperty(ARGEO_USER_ID, username);
			return userHome;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot create home node for user "
					+ username, e);
		}
	}

	protected String usernameToHomePath(String username) {
		return homeBasePath + '/' + JcrUtils.firstCharsToPath(username, 2)
				+ '/' + username;
	}

	public void mapUserToContext(UserDetails user, final DirContextAdapter ctx) {
		if (!(user instanceof JcrUserDetails))
			throw new ArgeoException("Unsupported user details: "
					+ user.getClass());

		ctx.setAttributeValues("objectClass", userClasses);
		ctx.setAttributeValue(usernameAttribute, user.getUsername());
		ctx.setAttributeValue(passwordAttribute, user.getPassword());

		final JcrUserDetails jcrUserDetails = (JcrUserDetails) user;
		systemExecutor.execute(new Runnable() {
			public void run() {
				Session session = null;
				try {
					Repository nodeRepo = JcrUtils.getRepositoryByAlias(
							repositoryFactory, ArgeoJcrConstants.ALIAS_NODE);
					session = nodeRepo.login();
					Node userProfile = session.getNode(jcrUserDetails
							.getHomePath() + '/' + ARGEO_USER_PROFILE);
					for (String jcrProperty : propertyToAttributes.keySet())
						jcrToLdap(userProfile, jcrProperty, ctx);
					if (log.isDebugEnabled())
						log.debug("Mapped " + userProfile + " to "
								+ ctx.getDn());
				} catch (RepositoryException e) {
					throw new ArgeoException("Cannot synchronize JCR and LDAP",
							e);
				} finally {
					session.logout();
				}
			}
		});
	}

	protected void ldapToJcr(Node userProfile, String jcrProperty,
			DirContextOperations ctx) {
		try {
			String ldapAttribute;
			if (propertyToAttributes.containsKey(jcrProperty))
				ldapAttribute = propertyToAttributes.get(jcrProperty);
			else
				throw new ArgeoException(
						"No LDAP attribute mapped for JCR proprty "
								+ jcrProperty);

			String value = ctx.getStringAttribute(ldapAttribute);
			if (value == null)
				return;
			userProfile.setProperty(jcrProperty, value);
		} catch (Exception e) {
			throw new ArgeoException("Cannot map JCR property " + jcrProperty
					+ " from LDAP", e);
		}
	}

	protected void jcrToLdap(Node userProfile, String jcrProperty,
			DirContextOperations ctx) {
		try {
			if (!userProfile.hasProperty(jcrProperty))
				return;
			String value = userProfile.getProperty(jcrProperty).getString();

			String ldapAttribute;
			if (propertyToAttributes.containsKey(jcrProperty))
				ldapAttribute = propertyToAttributes.get(jcrProperty);
			else
				throw new ArgeoException(
						"No LDAP attribute mapped for JCR proprty "
								+ jcrProperty);
			ctx.setAttributeValue(ldapAttribute, value);
		} catch (Exception e) {
			throw new ArgeoException("Cannot map JCR property " + jcrProperty
					+ " from LDAP", e);
		}
	}

	public void setPropertyToAttributes(Map<String, String> propertyToAttributes) {
		this.propertyToAttributes = propertyToAttributes;
	}

	public void setSystemExecutor(Executor systemExecutor) {
		this.systemExecutor = systemExecutor;
	}

	public void setHomeBasePath(String homeBasePath) {
		this.homeBasePath = homeBasePath;
	}

	public void register(RepositoryFactory repositoryFactory,
			Map<String, String> parameters) {
		this.repositoryFactory = repositoryFactory;
	}

	public void unregister(RepositoryFactory repositoryFactory,
			Map<String, String> parameters) {
		this.repositoryFactory = null;
	}

	public void setUsernameAttribute(String usernameAttribute) {
		this.usernameAttribute = usernameAttribute;
	}

	public void setPasswordAttribute(String passwordAttribute) {
		this.passwordAttribute = passwordAttribute;
	}

	public void setUserClasses(String[] userClasses) {
		this.userClasses = userClasses;
	}

}
