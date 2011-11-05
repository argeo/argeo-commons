package org.argeo.security.ldap.jcr;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import javax.naming.event.ObjectChangeListener;
import javax.naming.ldap.UnsolicitedNotification;
import javax.naming.ldap.UnsolicitedNotificationEvent;
import javax.naming.ldap.UnsolicitedNotificationListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.jcr.JcrUserDetails;
import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.ldap.LdapUsernameToDnMapper;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.ldap.UserDetailsContextMapper;

/** Guarantees that LDAP and JCR are in line. */
public class JcrLdapSynchronizer implements UserDetailsContextMapper,
		ArgeoNames {
	private final static Log log = LogFactory.getLog(JcrLdapSynchronizer.class);

	// LDAP
	private LdapTemplate ldapTemplate;
	/**
	 * LDAP template whose context source has an object factory set to null. see
	 * <a href=
	 * "http://forum.springsource.org/showthread.php?55955-Persistent-search-with-spring-ldap"
	 * >this</a>
	 */
	private LdapTemplate rawLdapTemplate;

	private String userBase;
	private String usernameAttribute;
	private String passwordAttribute;
	private String[] userClasses;

	private NamingListener ldapUserListener;
	private SearchControls subTreeSearchControls;
	private LdapUsernameToDnMapper usernameMapper;

	private PasswordEncoder passwordEncoder;
	private final Random random;

	// JCR
	/** Admin session on the security workspace */
	private Session securitySession;
	private Repository repository;

	private String securityWorkspace = "security";

	private JcrProfileListener jcrProfileListener;

	// Mapping
	private Map<String, String> propertyToAttributes = new HashMap<String, String>();

	public JcrLdapSynchronizer() {
		random = createRandom();
	}

	public void init() {
		try {
			securitySession = repository.login(securityWorkspace);

			synchronize();

			// LDAP
			subTreeSearchControls = new SearchControls();
			subTreeSearchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			// LDAP listener
			ldapUserListener = new LdapUserListener();
			rawLdapTemplate.executeReadOnly(new ContextExecutor() {
				public Object executeWithContext(DirContext ctx)
						throws NamingException {
					EventDirContext ectx = (EventDirContext) ctx.lookup("");
					ectx.addNamingListener(userBase, "(" + usernameAttribute
							+ "=*)", subTreeSearchControls, ldapUserListener);
					return null;
				}
			});

			// JCR
			String[] nodeTypes = { ArgeoTypes.ARGEO_USER_PROFILE };
			jcrProfileListener = new JcrProfileListener();
			// noLocal is used so that we are not notified when we modify JCR
			// from LDAP
			securitySession
					.getWorkspace()
					.getObservationManager()
					.addEventListener(jcrProfileListener,
							Event.PROPERTY_CHANGED | Event.NODE_ADDED, "/",
							true, null, nodeTypes, true);
		} catch (Exception e) {
			JcrUtils.logoutQuietly(securitySession);
			throw new ArgeoException("Cannot initialize LDAP/JCR synchronizer",
					e);
		}
	}

	public void destroy() {
		JcrUtils.removeListenerQuietly(securitySession, jcrProfileListener);
		JcrUtils.logoutQuietly(securitySession);
		try {
			rawLdapTemplate.executeReadOnly(new ContextExecutor() {
				public Object executeWithContext(DirContext ctx)
						throws NamingException {
					EventDirContext ectx = (EventDirContext) ctx.lookup("");
					ectx.removeNamingListener(ldapUserListener);
					return null;
				}
			});
		} catch (Exception e) {
			// silent (LDAP server may have been shutdown already)
			if (log.isTraceEnabled())
				log.trace("Cannot remove LDAP listener", e);
		}
	}

	/*
	 * LDAP TO JCR
	 */
	/** Full synchronization between LDAP and JCR. LDAP has priority. */
	protected void synchronize() {
		try {
			Name userBaseName = new DistinguishedName(userBase);
			// TODO subtree search?
			@SuppressWarnings("unchecked")
			List<String> userPaths = (List<String>) ldapTemplate.listBindings(
					userBaseName, new ContextMapper() {
						public Object mapFromContext(Object ctxObj) {
							return mapLdapToJcr((DirContextAdapter) ctxObj);
						}
					});

			// disable accounts which are not in LDAP
			Query query = securitySession
					.getWorkspace()
					.getQueryManager()
					.createQuery(
							"select * from [" + ArgeoTypes.ARGEO_USER_PROFILE
									+ "]", Query.JCR_SQL2);
			NodeIterator it = query.execute().getNodes();
			while (it.hasNext()) {
				Node userProfile = it.nextNode();
				String path = userProfile.getPath();
				if (!userPaths.contains(path)) {
					userProfile.setProperty(ArgeoNames.ARGEO_ENABLED, false);
				}
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot synchronized LDAP and JCR", e);
		}
	}

	/** Called during authentication in order to retrieve user details */
	public UserDetails mapUserFromContext(final DirContextOperations ctx,
			final String username, GrantedAuthority[] authorities) {
		if (ctx == null)
			throw new ArgeoException("No LDAP information for user " + username);
		Node userHome = JcrUtils.getUserHome(securitySession, username);
		if (userHome == null)
			throw new ArgeoException("No JCR information for user " + username);

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

		try {
			return new JcrUserDetails(userHome.getNode(ARGEO_PROFILE),
					password, authorities);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot retrieve user details for "
					+ username, e);
		}
	}

	/**
	 * Writes an LDAP context to the JCR user profile.
	 * 
	 * @return path to user profile
	 */
	protected String mapLdapToJcr(DirContextAdapter ctx) {
		Session session = securitySession;
		try {
			// process
			String username = ctx.getStringAttribute(usernameAttribute);
			Node userHome = JcrUtils.createUserHomeIfNeeded(session, username);
			Node userProfile; // = userHome.getNode(ARGEO_PROFILE);
			if (userHome.hasNode(ARGEO_PROFILE)) {
				userProfile = userHome.getNode(ARGEO_PROFILE);
			} else {
				userProfile = JcrUtils.createUserProfile(securitySession,
						username);
				userProfile.getSession().save();
				userProfile.getSession().getWorkspace().getVersionManager()
						.checkin(userProfile.getPath());
			}

			Map<String, String> modifications = new HashMap<String, String>();
			for (String jcrProperty : propertyToAttributes.keySet())
				ldapToJcr(userProfile, jcrProperty, ctx, modifications);

			// assign default values
			// if (!userProfile.hasProperty(Property.JCR_DESCRIPTION)
			// && !modifications.containsKey(Property.JCR_DESCRIPTION))
			// modifications.put(Property.JCR_DESCRIPTION, "");
			// if (!userProfile.hasProperty(Property.JCR_TITLE))
			// modifications.put(Property.JCR_TITLE,
			// userProfile.getProperty(ARGEO_FIRST_NAME).getString()
			// + " "
			// + userProfile.getProperty(ARGEO_LAST_NAME)
			// .getString());
			int modifCount = modifications.size();
			if (modifCount > 0) {
				session.getWorkspace().getVersionManager()
						.checkout(userProfile.getPath());
				for (String prop : modifications.keySet())
					userProfile.setProperty(prop, modifications.get(prop));
				JcrUtils.updateLastModified(userProfile);
				session.save();
				session.getWorkspace().getVersionManager()
						.checkin(userProfile.getPath());
				if (log.isDebugEnabled())
					log.debug("Mapped " + modifCount + " LDAP modification"
							+ (modifCount == 1 ? "" : "s") + " from "
							+ ctx.getDn() + " to " + userProfile);
			}
			return userProfile.getPath();
		} catch (Exception e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoException("Cannot synchronize JCR and LDAP", e);
		}
	}

	/** Maps an LDAP property to a JCR property */
	protected void ldapToJcr(Node userProfile, String jcrProperty,
			DirContextOperations ctx, Map<String, String> modifications) {
		// TODO do we really need DirContextOperations?
		try {
			String ldapAttribute;
			if (propertyToAttributes.containsKey(jcrProperty))
				ldapAttribute = propertyToAttributes.get(jcrProperty);
			else
				throw new ArgeoException(
						"No LDAP attribute mapped for JCR proprty "
								+ jcrProperty);

			String value = ctx.getStringAttribute(ldapAttribute);
			// if (value == null && Property.JCR_TITLE.equals(jcrProperty))
			// value = "";
			// if (value == null &&
			// Property.JCR_DESCRIPTION.equals(jcrProperty))
			// value = "";
			String jcrValue = userProfile.hasProperty(jcrProperty) ? userProfile
					.getProperty(jcrProperty).getString() : null;
			if (value != null && jcrValue != null) {
				if (!value.equals(jcrValue))
					modifications.put(jcrProperty, value);
			} else if (value != null && jcrValue == null) {
				modifications.put(jcrProperty, value);
			} else if (value == null && jcrValue != null) {
				modifications.put(jcrProperty, value);
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot map JCR property " + jcrProperty
					+ " from LDAP", e);
		}
	}

	/*
	 * JCR to LDAP
	 */

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
			Node userProfile = securitySession.getNode(
					jcrUserDetails.getHomePath()).getNode(ARGEO_PROFILE);
			for (String jcrProperty : propertyToAttributes.keySet()) {
				if (userProfile.hasProperty(jcrProperty)) {
					ModificationItem mi = jcrToLdap(jcrProperty, userProfile
							.getProperty(jcrProperty).getString());
					if (mi != null)
						ctx.setAttribute(mi.getAttribute());
				}
			}
			if (log.isTraceEnabled())
				log.trace("Mapped " + userProfile + " to " + ctx.getDn());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot synchronize JCR and LDAP", e);
		}

	}

	/** Maps a JCR property to an LDAP property */
	protected ModificationItem jcrToLdap(String jcrProperty, String value) {
		// TODO do we really need DirContextOperations?
		try {
			String ldapAttribute;
			if (propertyToAttributes.containsKey(jcrProperty))
				ldapAttribute = propertyToAttributes.get(jcrProperty);
			else
				return null;

			// fix issue with empty 'sn' in LDAP
			if (ldapAttribute.equals("sn") && (value.trim().equals("")))
				return null;
			// fix issue with empty 'description' in LDAP
			if (ldapAttribute.equals("description") && value.trim().equals(""))
				return null;
			BasicAttribute attr = new BasicAttribute(
					propertyToAttributes.get(jcrProperty), value);
			ModificationItem mi = new ModificationItem(
					DirContext.REPLACE_ATTRIBUTE, attr);
			return mi;
		} catch (Exception e) {
			throw new ArgeoException("Cannot map JCR property " + jcrProperty
					+ " from LDAP", e);
		}
	}

	/*
	 * UTILITIES
	 */
	protected String encodePassword(String password) {
		if (!password.startsWith("{")) {
			byte[] salt = new byte[16];
			random.nextBytes(salt);
			return passwordEncoder.encodePassword(password, salt);
		} else {
			return password;
		}
	}

	private static Random createRandom() {
		try {
			return SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			return new Random(System.currentTimeMillis());
		}
	}

	/*
	 * DEPENDENCY INJECTION
	 */

	public void setLdapTemplate(LdapTemplate ldapTemplate) {
		this.ldapTemplate = ldapTemplate;
	}

	public void setRawLdapTemplate(LdapTemplate rawLdapTemplate) {
		this.rawLdapTemplate = rawLdapTemplate;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setSecurityWorkspace(String securityWorkspace) {
		this.securityWorkspace = securityWorkspace;
	}

	public void setUserBase(String userBase) {
		this.userBase = userBase;
	}

	public void setUsernameAttribute(String usernameAttribute) {
		this.usernameAttribute = usernameAttribute;
	}

	public void setPropertyToAttributes(Map<String, String> propertyToAttributes) {
		this.propertyToAttributes = propertyToAttributes;
	}

	public void setUsernameMapper(LdapUsernameToDnMapper usernameMapper) {
		this.usernameMapper = usernameMapper;
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

	/** Listen to LDAP */
	class LdapUserListener implements ObjectChangeListener,
			NamespaceChangeListener, UnsolicitedNotificationListener {

		public void namingExceptionThrown(NamingExceptionEvent evt) {
			evt.getException().printStackTrace();
		}

		public void objectChanged(NamingEvent evt) {
			Binding user = evt.getNewBinding();
			// TODO find a way not to be called when JCR is the source of the
			// modification
			DirContextAdapter ctx = (DirContextAdapter) ldapTemplate
					.lookup(user.getName());
			mapLdapToJcr(ctx);
		}

		public void objectAdded(NamingEvent evt) {
			Binding user = evt.getNewBinding();
			DirContextAdapter ctx = (DirContextAdapter) ldapTemplate
					.lookup(user.getName());
			mapLdapToJcr(ctx);
		}

		public void objectRemoved(NamingEvent evt) {
			if (log.isDebugEnabled())
				log.debug(evt);
		}

		public void objectRenamed(NamingEvent evt) {
			if (log.isDebugEnabled())
				log.debug(evt);
		}

		public void notificationReceived(UnsolicitedNotificationEvent evt) {
			UnsolicitedNotification notification = evt.getNotification();
			NamingException ne = notification.getException();
			String msg = "LDAP notification " + "ID=" + notification.getID()
					+ ", referrals=" + notification.getReferrals();
			if (ne != null) {
				if (log.isTraceEnabled())
					log.trace(msg + ", exception= " + ne, ne);
				else
					log.warn(msg + ", exception= " + ne);
			} else if (log.isDebugEnabled()) {
				log.debug("Unsollicited LDAP notification " + msg);
			}
		}

	}

	/** Listen to JCR */
	class JcrProfileListener implements EventListener {

		public void onEvent(EventIterator events) {
			try {
				final Map<Name, List<ModificationItem>> modifications = new HashMap<Name, List<ModificationItem>>();
				while (events.hasNext()) {
					Event event = events.nextEvent();
					try {
						if (Event.PROPERTY_CHANGED == event.getType()) {
							Property property = (Property) securitySession
									.getItem(event.getPath());
							String propertyName = property.getName();
							Node userProfile = property.getParent();
							String username = userProfile.getProperty(
									ARGEO_USER_ID).getString();
							if (propertyToAttributes.containsKey(propertyName)) {
								Name name = usernameMapper.buildDn(username);
								if (!modifications.containsKey(name))
									modifications.put(name,
											new ArrayList<ModificationItem>());
								String value = property.getString();
								ModificationItem mi = jcrToLdap(propertyName,
										value);
								if (mi != null)
									modifications.get(name).add(mi);
							}
						} else if (Event.NODE_ADDED == event.getType()) {
							Node userProfile = securitySession.getNode(event
									.getPath());
							String username = userProfile.getProperty(
									ARGEO_USER_ID).getString();
							Name name = usernameMapper.buildDn(username);
							for (String propertyName : propertyToAttributes
									.keySet()) {
								if (!modifications.containsKey(name))
									modifications.put(name,
											new ArrayList<ModificationItem>());
								String value = userProfile.getProperty(
										propertyName).getString();
								ModificationItem mi = jcrToLdap(propertyName,
										value);
								if (mi != null)
									modifications.get(name).add(mi);
							}
						}
					} catch (RepositoryException e) {
						throw new ArgeoException("Cannot process event "
								+ event, e);
					}
				}

				for (Name name : modifications.keySet()) {
					List<ModificationItem> userModifs = modifications.get(name);
					int modifCount = userModifs.size();
					ldapTemplate.modifyAttributes(name, userModifs
							.toArray(new ModificationItem[modifCount]));
					if (log.isDebugEnabled())
						log.debug("Mapped " + modifCount + " JCR modification"
								+ (modifCount == 1 ? "" : "s") + " to " + name);
				}
			} catch (Exception e) {
				// if (log.isDebugEnabled())
				// e.printStackTrace();
				throw new ArgeoException("Cannot process JCR events ("
						+ e.getMessage() + ")", e);
			}
		}

	}
}
