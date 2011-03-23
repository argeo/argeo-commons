package org.argeo.security.ldap.jcr;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.jcr.JcrUserDetails;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.encoding.PasswordEncoder;
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
	private Session session;

	private PasswordEncoder passwordEncoder;
	private final Random random;

	public JcrUserDetailsContextMapper() {
		random = createRandom();
	}

	private static Random createRandom() {
		try {
			return SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			return new Random(System.currentTimeMillis());
		}
	}

	public UserDetails mapUserFromContext(final DirContextOperations ctx,
			final String username, GrantedAuthority[] authorities) {
		// if (repository == null)
		// throw new ArgeoException("No JCR repository registered");
		final StringBuffer userHomePathT = new StringBuffer("");
		Runnable action = new Runnable() {
			public void run() {
				String userHomepath = mapLdapToJcr(username, ctx);
				userHomePathT.append(userHomepath);
			}
		};

		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			// authentication
			systemExecutor.execute(action);
			JcrUtils.logoutQuietly(session);
		} else {
			// authenticated user
			action.run();
		}

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
		// Session session = null;
		try {
			// Repository nodeRepo = JcrUtils.getRepositoryByAlias(
			// repositoryFactory, ArgeoJcrConstants.ALIAS_NODE);
			// session = nodeRepo.login();
			Node userHome = JcrUtils.getUserHome(session, username);
			if (userHome == null)
				userHome = JcrUtils.createUserHome(session, homeBasePath,
						username);
			String userHomePath = userHome.getPath();
			Node userProfile = userHome.getNode(ARGEO_PROFILE);
			if (userHome.hasNode(ARGEO_PROFILE)) {
				userProfile = userHome.getNode(ARGEO_PROFILE);
			} else {
				userProfile = userHome.addNode(ARGEO_PROFILE);
				userProfile.addMixin(NodeType.MIX_TITLE);
				userProfile.addMixin(NodeType.MIX_CREATED);
				userProfile.addMixin(NodeType.MIX_LAST_MODIFIED);
			}
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
			// JcrUtils.logoutQuietly(session);
		}
	}

	public void mapUserToContext(UserDetails user, final DirContextAdapter ctx) {
		if (!(user instanceof JcrUserDetails))
			throw new ArgeoException("Unsupported user details: "
					+ user.getClass());

		ctx.setAttributeValues("objectClass", userClasses);
		ctx.setAttributeValue(usernameAttribute, user.getUsername());
		ctx.setAttributeValue(passwordAttribute,
				encodePassword(user.getPassword()));

		final JcrUserDetails jcrUserDetails = (JcrUserDetails) user;
		// systemExecutor.execute(new Runnable() {
		// public void run() {
		// Session session = null;
		try {
			// Repository nodeRepo = JcrUtils.getRepositoryByAlias(
			// repositoryFactory, ArgeoJcrConstants.ALIAS_NODE);
			// session = nodeRepo.login();
			Node userProfile = session.getNode(jcrUserDetails.getHomePath()
					+ '/' + ARGEO_PROFILE);
			for (String jcrProperty : propertyToAttributes.keySet())
				jcrToLdap(userProfile, jcrProperty, ctx);
			if (log.isDebugEnabled())
				log.debug("Mapped " + userProfile + " to " + ctx.getDn());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot synchronize JCR and LDAP", e);
		} finally {
			// session.logout();
		}
		// }
		// });
	}

	protected String encodePassword(String password) {
		if (!password.startsWith("{")) {
			byte[] salt = new byte[16];
			random.nextBytes(salt);
			return passwordEncoder.encodePassword(password, salt);
		} else {
			return password;
		}
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

	// public void register(RepositoryFactory repositoryFactory,
	// Map<String, String> parameters) {
	// this.repositoryFactory = repositoryFactory;
	// }
	//
	// public void unregister(RepositoryFactory repositoryFactory,
	// Map<String, String> parameters) {
	// this.repositoryFactory = null;
	// }

	public void setUsernameAttribute(String usernameAttribute) {
		this.usernameAttribute = usernameAttribute;
	}

	public void setPasswordAttribute(String passwordAttribute) {
		this.passwordAttribute = passwordAttribute;
	}

	public void setUserClasses(String[] userClasses) {
		this.userClasses = userClasses;
	}

	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
