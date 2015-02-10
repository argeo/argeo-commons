/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.security.ldap.jcr;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.version.VersionManager;
import javax.naming.Name;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.SecurityUtils;
import org.argeo.security.jcr.JcrSecurityModel;
import org.argeo.security.jcr.JcrUserDetails;
import org.argeo.security.jcr.SimpleJcrSecurityModel;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.LdapUsernameToDnMapper;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

/** Makes sure that LDAP and JCR are in line. */
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
	// private LdapTemplate rawLdapTemplate;

	private String userBase;
	private String usernameAttribute;
	private String passwordAttribute;
	private String[] userClasses;
	// private String defaultUserRole ="ROLE_USER";

	// private NamingListener ldapUserListener;
	// private SearchControls subTreeSearchControls;
	private LdapUsernameToDnMapper usernameMapper;

	private PasswordEncoder passwordEncoder;
	private final Random random;

	// JCR
	/** Admin session on the main workspace */
	private Session nodeSession;
	private Repository repository;

	// private JcrProfileListener jcrProfileListener;
	private JcrSecurityModel jcrSecurityModel = new SimpleJcrSecurityModel();

	// Mapping
	private Map<String, String> propertyToAttributes = new HashMap<String, String>();

	public JcrLdapSynchronizer() {
		random = createRandom();
	}

	public void init() {
		try {
			nodeSession = repository.login();

			// TODO put this in a different thread, and poll the LDAP server
			// until it is up
			try {
				synchronize();

				// LDAP
				// subTreeSearchControls = new SearchControls();
				// subTreeSearchControls
				// .setSearchScope(SearchControls.SUBTREE_SCOPE);
				// LDAP listener
				// ldapUserListener = new LdapUserListener();
				// rawLdapTemplate.executeReadOnly(new ContextExecutor() {
				// public Object executeWithContext(DirContext ctx)
				// throws NamingException {
				// EventDirContext ectx = (EventDirContext) ctx.lookup("");
				// ectx.addNamingListener(userBase, "("
				// + usernameAttribute + "=*)",
				// subTreeSearchControls, ldapUserListener);
				// return null;
				// }
				// });
			} catch (Exception e) {
				log.error("Could not synchronize and listen to LDAP,"
						+ " probably because the LDAP server is not available."
						+ " Restart the system as soon as possible.", e);
			}

			// JCR
			// String[] nodeTypes = { ArgeoTypes.ARGEO_USER_PROFILE };
			// jcrProfileListener = new JcrProfileListener();
			// noLocal is used so that we are not notified when we modify JCR
			// from LDAP
			// nodeSession
			// .getWorkspace()
			// .getObservationManager()
			// .addEventListener(jcrProfileListener,
			// Event.PROPERTY_CHANGED | Event.NODE_ADDED, "/",
			// true, null, nodeTypes, true);
		} catch (Exception e) {
			JcrUtils.logoutQuietly(nodeSession);
			throw new ArgeoException("Cannot initialize LDAP/JCR synchronizer",
					e);
		}
	}

	public void destroy() {
		// JcrUtils.removeListenerQuietly(nodeSession, jcrProfileListener);
		JcrUtils.logoutQuietly(nodeSession);
		// try {
		// rawLdapTemplate.executeReadOnly(new ContextExecutor() {
		// public Object executeWithContext(DirContext ctx)
		// throws NamingException {
		// EventDirContext ectx = (EventDirContext) ctx.lookup("");
		// ectx.removeNamingListener(ldapUserListener);
		// return null;
		// }
		// });
		// } catch (Exception e) {
		// // silent (LDAP server may have been shutdown already)
		// if (log.isTraceEnabled())
		// log.trace("Cannot remove LDAP listener", e);
		// }
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
							try {
								return mapLdapToJcr((DirContextAdapter) ctxObj);
							} catch (Exception e) {
								// do not break process because of error
								log.error(
										"Could not LDAP->JCR synchronize user "
												+ ctxObj, e);
								return null;
							}
						}
					});

			// create accounts which are not in LDAP
			Query query = nodeSession
					.getWorkspace()
					.getQueryManager()
					.createQuery(
							"select * from [" + ArgeoTypes.ARGEO_USER_PROFILE
									+ "]", Query.JCR_SQL2);
			NodeIterator it = query.execute().getNodes();
			while (it.hasNext()) {
				Node userProfile = it.nextNode();
				String path = userProfile.getPath();
				try {
					if (!userPaths.contains(path)) {
						String username = userProfile
								.getProperty(ARGEO_USER_ID).getString();
						// GrantedAuthority[] authorities = {new
						// GrantedAuthorityImpl(defaultUserRole)};
						List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
						JcrUserDetails userDetails = new JcrUserDetails(
								userProfile, username, authorities);
						String dn = createLdapUser(userDetails);
						log.warn("Created ldap entry '" + dn + "' for user '"
								+ username + "'");

						// if(!userProfile.getProperty(ARGEO_ENABLED).getBoolean()){
						// continue profiles;
						// }
						//
						// log.warn("Path "
						// + path
						// + " not found in LDAP, disabling user "
						// + userProfile.getProperty(ArgeoNames.ARGEO_USER_ID)
						// .getString());

						// Temporary hack to repair previous behaviour
						if (!userProfile.getProperty(ARGEO_ENABLED)
								.getBoolean()) {
							VersionManager versionManager = nodeSession
									.getWorkspace().getVersionManager();
							versionManager.checkout(userProfile.getPath());
							userProfile.setProperty(ArgeoNames.ARGEO_ENABLED,
									true);
							nodeSession.save();
							versionManager.checkin(userProfile.getPath());
						}
					}
				} catch (Exception e) {
					log.error("Cannot process " + path, e);
				}
			}
		} catch (Exception e) {
			JcrUtils.discardQuietly(nodeSession);
			log.error("Cannot synchronize LDAP and JCR", e);
			// throw new ArgeoException("Cannot synchronize LDAP and JCR", e);
		}
	}

	private String createLdapUser(UserDetails user) {
		DirContextAdapter ctx = new DirContextAdapter();
		mapUserToContext(user, ctx);
		DistinguishedName dn = usernameMapper.buildDn(user.getUsername());
		ldapTemplate.bind(dn, ctx, null);
		return dn.toString();
	}

	/** Called during authentication in order to retrieve user details */
	public UserDetails mapUserFromContext(final DirContextOperations ctx,
			final String username,
			Collection<? extends GrantedAuthority> authorities) {
		if (ctx == null)
			throw new ArgeoException("No LDAP information for user " + username);

		String ldapUsername = ctx.getStringAttribute(usernameAttribute);
		if (!ldapUsername.equals(username))
			throw new ArgeoException("Logged in with username " + username
					+ " but LDAP user is " + ldapUsername);

		Node userProfile = jcrSecurityModel.sync(nodeSession, username,
				SecurityUtils.authoritiesToStringList(authorities));
		// JcrUserDetails.checkAccountStatus(userProfile);

		// password
		SortedSet<?> passwordAttributes = ctx
				.getAttributeSortedStringSet(passwordAttribute);
		String password;
		if (passwordAttributes == null || passwordAttributes.size() == 0) {
			// throw new ArgeoException("No password found for user " +
			// username);
			password = "NULL";
		} else {
			byte[] arr = (byte[]) passwordAttributes.first();
			password = new String(arr);
			// erase password
			Arrays.fill(arr, (byte) 0);
		}

		try {
			return new JcrUserDetails(userProfile, password, authorities);
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
	protected synchronized String mapLdapToJcr(DirContextAdapter ctx) {
		Session session = nodeSession;
		try {
			// process
			String username = ctx.getStringAttribute(usernameAttribute);

			Node userProfile = jcrSecurityModel.sync(session, username, null);
			Map<String, String> modifications = new HashMap<String, String>();
			for (String jcrProperty : propertyToAttributes.keySet())
				ldapToJcr(userProfile, jcrProperty, ctx, modifications);

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
			Node userProfile = nodeSession
					.getNode(jcrUserDetails.getHomePath()).getNode(
							ARGEO_PROFILE);
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
		// this.rawLdapTemplate = rawLdapTemplate;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
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

	public void setJcrSecurityModel(JcrSecurityModel jcrSecurityModel) {
		this.jcrSecurityModel = jcrSecurityModel;
	}

	/** Listen to LDAP */
	// class LdapUserListener implements ObjectChangeListener,
	// NamespaceChangeListener, UnsolicitedNotificationListener {
	//
	// public void namingExceptionThrown(NamingExceptionEvent evt) {
	// evt.getException().printStackTrace();
	// }
	//
	// public void objectChanged(NamingEvent evt) {
	// Binding user = evt.getNewBinding();
	// // TODO find a way not to be called when JCR is the source of the
	// // modification
	// DirContextAdapter ctx = (DirContextAdapter) ldapTemplate
	// .lookup(user.getName());
	// mapLdapToJcr(ctx);
	// }
	//
	// public void objectAdded(NamingEvent evt) {
	// Binding user = evt.getNewBinding();
	// DirContextAdapter ctx = (DirContextAdapter) ldapTemplate
	// .lookup(user.getName());
	// mapLdapToJcr(ctx);
	// }
	//
	// public void objectRemoved(NamingEvent evt) {
	// if (log.isDebugEnabled())
	// log.debug(evt);
	// }
	//
	// public void objectRenamed(NamingEvent evt) {
	// if (log.isDebugEnabled())
	// log.debug(evt);
	// }
	//
	// public void notificationReceived(UnsolicitedNotificationEvent evt) {
	// UnsolicitedNotification notification = evt.getNotification();
	// NamingException ne = notification.getException();
	// String msg = "LDAP notification " + "ID=" + notification.getID()
	// + ", referrals=" + notification.getReferrals();
	// if (ne != null) {
	// if (log.isTraceEnabled())
	// log.trace(msg + ", exception= " + ne, ne);
	// else
	// log.warn(msg + ", exception= " + ne);
	// } else if (log.isDebugEnabled()) {
	// log.debug("Unsollicited LDAP notification " + msg);
	// }
	// }
	//
	// }

	/** Listen to JCR */
	// class JcrProfileListener implements EventListener {
	//
	// public void onEvent(EventIterator events) {
	// try {
	// final Map<Name, List<ModificationItem>> modifications = new HashMap<Name,
	// List<ModificationItem>>();
	// while (events.hasNext()) {
	// Event event = events.nextEvent();
	// try {
	// if (Event.PROPERTY_CHANGED == event.getType()) {
	// Property property = (Property) nodeSession
	// .getItem(event.getPath());
	// String propertyName = property.getName();
	// Node userProfile = property.getParent();
	// String username = userProfile.getProperty(
	// ARGEO_USER_ID).getString();
	// if (propertyToAttributes.containsKey(propertyName)) {
	// Name name = usernameMapper.buildDn(username);
	// if (!modifications.containsKey(name))
	// modifications.put(name,
	// new ArrayList<ModificationItem>());
	// String value = property.getString();
	// ModificationItem mi = jcrToLdap(propertyName,
	// value);
	// if (mi != null)
	// modifications.get(name).add(mi);
	// }
	// } else if (Event.NODE_ADDED == event.getType()) {
	// Node userProfile = nodeSession.getNode(event
	// .getPath());
	// String username = userProfile.getProperty(
	// ARGEO_USER_ID).getString();
	// Name name = usernameMapper.buildDn(username);
	// for (String propertyName : propertyToAttributes
	// .keySet()) {
	// if (!modifications.containsKey(name))
	// modifications.put(name,
	// new ArrayList<ModificationItem>());
	// String value = userProfile.getProperty(
	// propertyName).getString();
	// ModificationItem mi = jcrToLdap(propertyName,
	// value);
	// if (mi != null)
	// modifications.get(name).add(mi);
	// }
	// }
	// } catch (RepositoryException e) {
	// throw new ArgeoException("Cannot process event "
	// + event, e);
	// }
	// }
	//
	// for (Name name : modifications.keySet()) {
	// List<ModificationItem> userModifs = modifications.get(name);
	// int modifCount = userModifs.size();
	// ldapTemplate.modifyAttributes(name, userModifs
	// .toArray(new ModificationItem[modifCount]));
	// if (log.isDebugEnabled())
	// log.debug("Mapped " + modifCount + " JCR modification"
	// + (modifCount == 1 ? "" : "s") + " to " + name);
	// }
	// } catch (Exception e) {
	// // if (log.isDebugEnabled())
	// // e.printStackTrace();
	// throw new ArgeoException("Cannot process JCR events ("
	// + e.getMessage() + ")", e);
	// }
	// }
	//
	// }
}
