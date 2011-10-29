package org.argeo.security.ldap.jcr;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.concurrent.Executor;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.jcr.JcrUserDetails;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.BadCredentialsException;
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

	/** 0 is always sync */
	private Long syncLatency = 10 * 60 * 1000l;

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
		if (ctx == null)
			throw new ArgeoException("No LDAP information found for user "
					+ username);

		final StringBuffer userHomePathT = new StringBuffer("");
		Runnable action = new Runnable() {
			public void run() {
				String userHomepath = mapLdapToJcr(username, ctx);
				userHomePathT.append(userHomepath);
			}
		};

		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			// authentication
			try {
				systemExecutor.execute(action);
			} finally {
				JcrUtils.logoutQuietly(session);
			}
		} else {
			// authenticated user
			action.run();
		}

		// password
		SortedSet<?> passwordAttributes = ctx
				.getAttributeSortedStringSet(passwordAttribute);
		String password;
		if (passwordAttributes == null || passwordAttributes.size() == 0) {
			throw new ArgeoException("No password found for user " + username);
		} else {
			byte[] arr = (byte[]) passwordAttributes.first();
			password = new String(arr);
			// erase password
			Arrays.fill(arr, (byte) 0);
		}
		JcrUserDetails userDetails = new JcrUserDetails(
				userHomePathT.toString(), username, password, true, true, true,
				true, authorities);
		return userDetails;
	}

	/** @return path to the user home node */
	protected synchronized String mapLdapToJcr(String username,
			DirContextOperations ctx) {
		String usernameLdap = ctx.getStringAttribute(usernameAttribute);
		// log.debug("username=" + username + ", usernameLdap=" + usernameLdap);
		if (!username.equals(usernameLdap)) {
			String msg = "Provided username '" + username
					+ "' is different from username stored in LDAP '"
					+ usernameLdap + "'";
			// we log it because the exception may not be displayed
			log.error(msg);
			throw new BadCredentialsException(msg);
		}

		try {

			Node userHome = JcrUtils.getUserHome(session, username);
			boolean justCreatedHome = false;
			if (userHome == null) {
				userHome = JcrUtils.createUserHome(session, homeBasePath,
						username);
				justCreatedHome = true;
			}
			String userHomePath = userHome.getPath();
			Node userProfile; // = userHome.getNode(ARGEO_PROFILE);
			if (userHome.hasNode(ARGEO_PROFILE)) {
				userProfile = userHome.getNode(ARGEO_PROFILE);
				if (syncLatency != 0 && !justCreatedHome) {
					Calendar lastModified = userProfile.getProperty(
							Property.JCR_LAST_MODIFIED).getDate();
					long timeSinceLastUpdate = System.currentTimeMillis()
							- lastModified.getTimeInMillis();
					if (timeSinceLastUpdate < syncLatency)// skip sync
						return userHomePath;
				}
			} else {
				throw new ArgeoException("We should never reach this point");
				// userProfile = userHome.addNode(ARGEO_PROFILE);
				// userProfile.addMixin(NodeType.MIX_TITLE);
				// userProfile.addMixin(NodeType.MIX_CREATED);
				// userProfile.addMixin(NodeType.MIX_LAST_MODIFIED);
			}

			session.getWorkspace().getVersionManager()
					.checkout(userProfile.getPath());
			for (String jcrProperty : propertyToAttributes.keySet())
				ldapToJcr(userProfile, jcrProperty, ctx);

			// assign default values
			if (!userProfile.hasProperty(Property.JCR_DESCRIPTION))
				userProfile.setProperty(Property.JCR_DESCRIPTION, "");
			if (!userProfile.hasProperty(Property.JCR_TITLE))
				userProfile.setProperty(Property.JCR_TITLE, userProfile
						.getProperty(ARGEO_FIRST_NAME).getString()
						+ " "
						+ userProfile.getProperty(ARGEO_LAST_NAME).getString());
			JcrUtils.updateLastModified(userProfile);
			session.save();
			session.getWorkspace().getVersionManager()
					.checkin(userProfile.getPath());
			if (log.isTraceEnabled())
				log.trace("Mapped " + ctx.getDn() + " to " + userProfile);
			return userHomePath;
		} catch (Exception e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoException("Cannot synchronize JCR and LDAP", e);
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
		try {
			Node userProfile = session.getNode(jcrUserDetails.getHomePath()
					+ '/' + ARGEO_PROFILE);
			for (String jcrProperty : propertyToAttributes.keySet())
				jcrToLdap(userProfile, jcrProperty, ctx);

			if (log.isTraceEnabled())
				log.trace("Mapped " + userProfile + " to " + ctx.getDn());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot synchronize JCR and LDAP", e);
		}
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
			String jcrValue = userProfile.hasProperty(jcrProperty) ? userProfile
					.getProperty(jcrProperty).getString() : null;
			if (value != null && jcrValue != null) {
				if (!value.equals(jcrValue))
					userProfile.setProperty(jcrProperty, value);
			} else if (value != null && jcrValue == null) {
				userProfile.setProperty(jcrProperty, value);
			} else if (value == null && jcrValue != null) {
				userProfile.setProperty(jcrProperty, value);
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot map JCR property " + jcrProperty
					+ " from LDAP", e);
		}
	}

	protected void jcrToLdap(Node userProfile, String jcrProperty,
			DirContextOperations ctx) {
		try {
			String ldapAttribute;
			if (propertyToAttributes.containsKey(jcrProperty))
				ldapAttribute = propertyToAttributes.get(jcrProperty);
			else
				throw new ArgeoException(
						"No LDAP attribute mapped for JCR proprty "
								+ jcrProperty);

			// fix issue with empty 'sn' in LDAP
			if (ldapAttribute.equals("sn")
					&& (!userProfile.hasProperty(jcrProperty) || userProfile
							.getProperty(jcrProperty).getString().trim()
							.equals("")))
				userProfile.setProperty(jcrProperty, "empty");

			if (ldapAttribute.equals("description")) {
				String value = userProfile.getProperty(jcrProperty).getString();
				if (value.trim().equals(""))
					return;
			}

			if (!userProfile.hasProperty(jcrProperty))
				return;
			String value = userProfile.getProperty(jcrProperty).getString();

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

	/**
	 * Time in ms during which the LDAP server is not checked. 0 is always sync.
	 */
	public void setSyncLatency(Long syncLatency) {
		this.syncLatency = syncLatency;
	}

}
