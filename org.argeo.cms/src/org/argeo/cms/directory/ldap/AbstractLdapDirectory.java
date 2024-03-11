package org.argeo.cms.directory.ldap;

import static org.argeo.api.acr.ldap.LdapNameUtils.toLdapName;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.transaction.xa.XAResource;

import org.argeo.api.acr.ldap.LdapAttr;
import org.argeo.api.acr.ldap.LdapNameUtils;
import org.argeo.api.acr.ldap.LdapObj;
import org.argeo.api.cms.directory.CmsDirectory;
import org.argeo.api.cms.directory.HierarchyUnit;
import org.argeo.api.cms.transaction.WorkControl;
import org.argeo.api.cms.transaction.WorkingCopyXaResource;
import org.argeo.api.cms.transaction.XAResourceProvider;
import org.argeo.cms.osgi.useradmin.OsUserDirectory;
import org.argeo.cms.runtime.DirectoryConf;

/** A {@link CmsDirectory} based either on LDAP or LDIF. */
public abstract class AbstractLdapDirectory implements CmsDirectory, XAResourceProvider {
	protected static final String SHARED_STATE_USERNAME = "javax.security.auth.login.name";
	protected static final String SHARED_STATE_PASSWORD = "javax.security.auth.login.password";

	private final LdapName baseDn;
	private final Hashtable<String, Object> configProperties;
	private final Rdn userBaseRdn, groupBaseRdn, systemRoleBaseRdn;
	private final String userObjectClass, groupObjectClass;
	private String memberAttributeId = "member";

	private final boolean readOnly;
	private final boolean disabled;
	private final String uri;

	private String forcedPassword;

	private final boolean scoped;

	private List<String> credentialAttributeIds = Arrays
			.asList(new String[] { LdapAttr.userPassword.name(), LdapAttr.authPassword.name() });

	private WorkControl transactionControl;
	private WorkingCopyXaResource<LdapEntryWorkingCopy> xaResource;

	private LdapDirectoryDao directoryDao;

	/** Whether the the directory has is authenticated via a service user. */
	private boolean authenticated = false;

	public AbstractLdapDirectory(URI uriArg, Dictionary<String, ?> props, boolean scoped) {
		this.configProperties = new Hashtable<String, Object>();
		for (Enumeration<String> keys = props.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement();
			configProperties.put(key, props.get(key));
		}

		String baseDnStr = DirectoryConf.baseDn.getValue(configProperties);
		if (baseDnStr == null)
			throw new IllegalArgumentException("Base DN must be specified: " + configProperties);
		baseDn = toLdapName(baseDnStr);
		this.scoped = scoped;

		if (uriArg != null) {
			uri = uriArg.toString();
			// uri from properties is ignored
		} else {
			String uriStr = DirectoryConf.uri.getValue(configProperties);
			if (uriStr == null)
				uri = null;
			else
				uri = uriStr;
		}

		forcedPassword = DirectoryConf.forcedPassword.getValue(configProperties);

		userObjectClass = DirectoryConf.userObjectClass.getValue(configProperties);
		groupObjectClass = DirectoryConf.groupObjectClass.getValue(configProperties);

		String userBase = DirectoryConf.userBase.getValue(configProperties);
		String groupBase = DirectoryConf.groupBase.getValue(configProperties);
		String systemRoleBase = DirectoryConf.systemRoleBase.getValue(configProperties);
		try {
//			baseDn = new LdapName(UserAdminConf.baseDn.getValue(properties));
			userBaseRdn = new Rdn(userBase);
//			userBaseDn = new LdapName(userBase + "," + baseDn);
			groupBaseRdn = new Rdn(groupBase);
//			groupBaseDn = new LdapName(groupBase + "," + baseDn);
			systemRoleBaseRdn = new Rdn(systemRoleBase);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException(
					"Badly formated base DN " + DirectoryConf.baseDn.getValue(configProperties), e);
		}

		// read only
		String readOnlyStr = DirectoryConf.readOnly.getValue(configProperties);
		if (readOnlyStr == null) {
			readOnly = readOnlyDefault(uri);
			configProperties.put(DirectoryConf.readOnly.name(), Boolean.toString(readOnly));
		} else
			readOnly = Boolean.parseBoolean(readOnlyStr);

		// disabled
		String disabledStr = DirectoryConf.disabled.getValue(configProperties);
		if (disabledStr != null)
			disabled = Boolean.parseBoolean(disabledStr);
		else
			disabled = false;
		if (!getRealm().isEmpty()) {
			// IPA multiple LDAP causes URI parsing to fail
			// TODO manage generic redundant LDAP case
			directoryDao = new LdapDao(this);
		} else {
			if (uri != null) {
				URI u = URI.create(uri);
				if (DirectoryConf.SCHEME_LDAP.equals(u.getScheme())
						|| DirectoryConf.SCHEME_LDAPS.equals(u.getScheme())) {
					directoryDao = new LdapDao(this);
					authenticated = configProperties.get(Context.SECURITY_PRINCIPAL) != null;
				} else if (DirectoryConf.SCHEME_FILE.equals(u.getScheme())) {
					directoryDao = new LdifDao(this);
					authenticated = true;
				} else if (DirectoryConf.SCHEME_OS.equals(u.getScheme())) {
					directoryDao = new OsUserDirectory(this);
					authenticated = true;
					// singleUser = true;
				} else {
					throw new IllegalArgumentException("Unsupported scheme " + u.getScheme());
				}
			} else {
				// in memory
				directoryDao = new LdifDao(this);
			}
		}
		if (directoryDao != null)
			xaResource = new WorkingCopyXaResource<>(directoryDao);
	}

	/*
	 * INITIALISATION
	 */

	public void init() {
		getDirectoryDao().init();
	}

	public void destroy() {
		getDirectoryDao().destroy();
	}

	/*
	 * CREATION
	 */
	protected abstract LdapEntry newUser(LdapName name);

	protected abstract LdapEntry newGroup(LdapName name);

	/*
	 * EDITION
	 */

	public boolean isEditing() {
		return xaResource.wc() != null;
	}

	public LdapEntryWorkingCopy getWorkingCopy() {
		LdapEntryWorkingCopy wc = xaResource.wc();
		if (wc == null)
			return null;
		return wc;
	}

	public void checkEdit() {
		if (xaResource.wc() == null) {
			try {
				transactionControl.getWorkContext().registerXAResource(xaResource, null);
			} catch (Exception e) {
				throw new IllegalStateException("Cannot enlist " + xaResource, e);
			}
		} else {
		}
	}

	public void setTransactionControl(WorkControl transactionControl) {
		this.transactionControl = transactionControl;
	}

	public XAResource getXaResource() {
		return xaResource;
	}

	public boolean removeEntry(LdapName dn) {
		checkEdit();
		LdapEntryWorkingCopy wc = getWorkingCopy();
		boolean actuallyDeleted;
		if (getDirectoryDao().entryExists(dn) || wc.getNewData().containsKey(dn)) {
			LdapEntry user = doGetRole(dn);
			wc.getDeletedData().put(dn, user);
			actuallyDeleted = true;
		} else {// just removing from groups (e.g. system roles)
			actuallyDeleted = false;
		}
		for (LdapName groupDn : getDirectoryDao().getDirectGroups(dn)) {
			LdapEntry group = doGetRole(groupDn);
			group.getAttributes().get(getMemberAttributeId()).remove(dn.toString());
		}
		return actuallyDeleted;
	}

	/*
	 * RETRIEVAL
	 */

	protected LdapEntry doGetRole(LdapName dn) {
		LdapEntryWorkingCopy wc = getWorkingCopy();
		LdapEntry user;
		try {
			user = getDirectoryDao().doGetEntry(dn);
		} catch (NameNotFoundException e) {
			user = null;
		}
		if (wc != null) {
			if (user == null && wc.getNewData().containsKey(dn))
				user = wc.getNewData().get(dn);
			else if (wc.getDeletedData().containsKey(dn))
				user = null;
		}
		return user;
	}

	protected void collectGroups(LdapEntry user, List<LdapEntry> allRoles) {
		Attributes attrs = user.getAttributes();
		// TODO centralize attribute name
		Attribute memberOf = attrs.get(LdapAttr.memberOf.name());
		// if user belongs to this directory, we only check memberOf
		if (memberOf != null && user.getDn().startsWith(getBaseDn())) {
			try {
				NamingEnumeration<?> values = memberOf.getAll();
				while (values.hasMore()) {
					Object value = values.next();
					LdapName groupDn = new LdapName(value.toString());
					LdapEntry group = doGetRole(groupDn);
					if (group != null) {
						allRoles.add(group);
					} else {
						// user doesn't have the right to retrieve role, but we know it exists
						// otherwise memberOf would not work
						group = newGroup(groupDn);
						allRoles.add(group);
					}
				}
			} catch (NamingException e) {
				throw new IllegalStateException("Cannot get memberOf groups for " + user, e);
			}
		} else {
			directGroups: for (LdapName groupDn : getDirectoryDao().getDirectGroups(user.getDn())) {
				LdapEntry group = doGetRole(groupDn);
				if (group != null) {
					if (allRoles.contains(group)) {
						// important in order to avoi loops
						continue directGroups;
					}
					allRoles.add(group);
					collectGroups(group, allRoles);
				}
			}
		}
	}

	/*
	 * HIERARCHY
	 */
	@Override
	public HierarchyUnit getHierarchyUnit(String path) {
		LdapName dn = pathToName(path);
		return directoryDao.doGetHierarchyUnit(dn);
	}

	@Override
	public Iterable<HierarchyUnit> getDirectHierarchyUnits(boolean functionalOnly) {
		return directoryDao.doGetDirectHierarchyUnits(baseDn, functionalOnly);
	}

	@Override
	public HierarchyUnit getDirectChild(Type type) {
		// TODO factorise with hierarchy unit?
		return switch (type) {
		case ROLES -> getDirectoryDao().doGetHierarchyUnit((LdapName) getBaseDn().add(getSystemRoleBaseRdn()));
		case PEOPLE -> getDirectoryDao().doGetHierarchyUnit((LdapName) getBaseDn().add(getUserBaseRdn()));
		case GROUPS -> getDirectoryDao().doGetHierarchyUnit((LdapName) getBaseDn().add(getGroupBaseRdn()));
		case FUNCTIONAL -> throw new IllegalArgumentException("Type must be a technical type");
		};
	}

	@Override
	public String getHierarchyUnitName() {
		return getName();
	}

	@Override
	public String getHierarchyUnitLabel(Locale locale) {
		String key = LdapNameUtils.getLastRdn(getBaseDn()).getType();
		Object value = LdapEntry.getLocalized(asLdapEntry().getProperties(), key, locale);
		if (value == null)
			value = getHierarchyUnitName();
		assert value != null;
		return value.toString();
	}

	@Override
	public HierarchyUnit getParent() {
		return null;
	}

	@Override
	public boolean isType(Type type) {
		return Type.FUNCTIONAL.equals(type);
	}

	@Override
	public CmsDirectory getDirectory() {
		return this;
	}

	@Override
	public HierarchyUnit createHierarchyUnit(String path) {
		checkEdit();
		LdapEntryWorkingCopy wc = getWorkingCopy();
		LdapName dn = pathToName(path);
		if ((getDirectoryDao().entryExists(dn) && !wc.getDeletedData().containsKey(dn))
				|| wc.getNewData().containsKey(dn))
			throw new IllegalArgumentException("Already a hierarchy unit " + path);
		BasicAttributes attrs = new BasicAttributes(true);
		attrs.put(LdapAttr.objectClass.name(), LdapObj.organizationalUnit.name());
		Rdn nameRdn = dn.getRdn(dn.size() - 1);
		// TODO deal with multiple attr RDN
		attrs.put(nameRdn.getType(), nameRdn.getValue());
		wc.getModifiedData().put(dn, attrs);
		LdapHierarchyUnit newHierarchyUnit = new LdapHierarchyUnit(this, dn);
		wc.getNewData().put(dn, newHierarchyUnit);
		return newHierarchyUnit;
	}

	/*
	 * PATHS
	 */

	@Override
	public String getBase() {
		return getBaseDn().toString();
	}

	@Override
	public String getName() {
		return nameToSimple(getBaseDn(), ".");
	}

	protected String nameToRelativePath(LdapName dn) {
		LdapName name = LdapNameUtils.relativeName(getBaseDn(), dn);
		return nameToSimple(name, "/");
	}

	protected String nameToSimple(LdapName name, String separator) {
		StringJoiner path = new StringJoiner(separator);
		for (int i = 0; i < name.size(); i++) {
			path.add(name.getRdn(i).getValue().toString());
		}
		return path.toString();

	}

	protected LdapName pathToName(String path) {
		try {
			LdapName name = (LdapName) getBaseDn().clone();
			String[] segments = path.split("/");
			Rdn parentRdn = null;
			// segments[0] is the directory itself
			for (int i = 0; i < segments.length; i++) {
				String segment = segments[i];
				// TODO make attr names configurable ?
				String attr = getDirectory().getRealm().isPresent()/* IPA */ ? LdapAttr.cn.name() : LdapAttr.ou.name();
				if (parentRdn != null) {
					if (getUserBaseRdn().equals(parentRdn))
						attr = LdapAttr.uid.name();
					else if (getGroupBaseRdn().equals(parentRdn))
						attr = LdapAttr.cn.name();
					else if (getSystemRoleBaseRdn().equals(parentRdn))
						attr = LdapAttr.cn.name();
				}
				Rdn rdn = new Rdn(attr, segment);
				name.add(rdn);
				parentRdn = rdn;
			}
			return name;
		} catch (InvalidNameException e) {
			throw new IllegalStateException("Cannot convert " + path + " to LDAP name", e);
		}

	}

	/*
	 * UTILITIES
	 */
	protected boolean isExternal(LdapName name) {
		return !name.startsWith(baseDn);
	}

	protected static boolean hasObjectClass(Attributes attrs, LdapObj objectClass) {
		return hasObjectClass(attrs, objectClass.name());
	}

	protected static boolean hasObjectClass(Attributes attrs, String objectClass) {
		try {
			Attribute attr = attrs.get(LdapAttr.objectClass.name());
			NamingEnumeration<?> en = attr.getAll();
			while (en.hasMore()) {
				String v = en.next().toString();
				if (v.equalsIgnoreCase(objectClass))
					return true;

			}
			return false;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot search for objectClass " + objectClass, e);
		}
	}

	private static boolean readOnlyDefault(String uriStr) {
		if (uriStr == null)
			return true;
		/// TODO make it more generic
		URI uri;
		try {
			uri = new URI(uriStr.split(" ")[0]);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
		if (uri.getScheme() == null)
			return false;// assume relative file to be writable
		if (uri.getScheme().equals(DirectoryConf.SCHEME_FILE)) {
			File file = new File(uri);
			if (file.exists())
				return !file.canWrite();
			else
				return !file.getParentFile().canWrite();
		} else if (uri.getScheme().equals(DirectoryConf.SCHEME_LDAP)) {
			if (uri.getAuthority() != null)// assume writable if authenticated
				return false;
		} else if (uri.getScheme().equals(DirectoryConf.SCHEME_OS)) {
			return true;
		}
		return true;// read only by default
	}

	/*
	 * AS AN ENTRY
	 */
	public LdapEntry asLdapEntry() {
		try {
			return directoryDao.doGetEntry(baseDn);
		} catch (NameNotFoundException e) {
			throw new IllegalStateException("Cannot get " + baseDn + " entry", e);
		}
	}

	public Dictionary<String, Object> getProperties() {
		return asLdapEntry().getProperties();
	}

	/*
	 * ACCESSORS
	 */
	@Override
	public Optional<String> getRealm() {
		Object realm = configProperties.get(DirectoryConf.realm.name());
		if (realm == null)
			return Optional.empty();
		return Optional.of(realm.toString());
	}

	public LdapName getBaseDn() {
		return (LdapName) baseDn.clone();
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public Rdn getUserBaseRdn() {
		return userBaseRdn;
	}

	public Rdn getGroupBaseRdn() {
		return groupBaseRdn;
	}

	public Rdn getSystemRoleBaseRdn() {
		return systemRoleBaseRdn;
	}

//	public Dictionary<String, Object> getConfigProperties() {
//		return configProperties;
//	}

	public Dictionary<String, Object> cloneConfigProperties() {
		return new Hashtable<>(configProperties);
	}

	public String getForcedPassword() {
		return forcedPassword;
	}

	public boolean isScoped() {
		return scoped;
	}

	public List<String> getCredentialAttributeIds() {
		return credentialAttributeIds;
	}

	public String getUri() {
		return uri;
	}

	public LdapDirectoryDao getDirectoryDao() {
		return directoryDao;
	}

	/** dn can be null, in that case a default should be returned. */
	public String getUserObjectClass() {
		return userObjectClass;
	}

	public String getGroupObjectClass() {
		return groupObjectClass;
	}

	public String getMemberAttributeId() {
		return memberAttributeId;
	}

	/*
	 * OBJECT METHODS
	 */

	@Override
	public int hashCode() {
		return baseDn.hashCode();
	}

	@Override
	public String toString() {
		return "Directory " + baseDn.toString();
	}

}
