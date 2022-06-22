package org.argeo.util.directory.ldap;

import static org.argeo.util.directory.ldap.LdapNameUtils.toLdapName;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.transaction.xa.XAResource;

import org.argeo.util.directory.Directory;
import org.argeo.util.directory.DirectoryConf;
import org.argeo.util.directory.HierarchyUnit;
import org.argeo.util.naming.LdapAttrs;
import org.argeo.util.naming.LdapObjs;
import org.argeo.util.transaction.WorkControl;
import org.argeo.util.transaction.WorkingCopyProcessor;
import org.argeo.util.transaction.WorkingCopyXaResource;
import org.argeo.util.transaction.XAResourceProvider;
import org.osgi.framework.Filter;

public abstract class AbstractLdapDirectory
		implements Directory, WorkingCopyProcessor<LdapEntryWorkingCopy>, XAResourceProvider {
	protected static final String SHARED_STATE_USERNAME = "javax.security.auth.login.name";
	protected static final String SHARED_STATE_PASSWORD = "javax.security.auth.login.password";

	protected final LdapName baseDn;
	protected final Hashtable<String, Object> properties;
	private final Rdn userBaseRdn, groupBaseRdn, systemRoleBaseRdn;
	private final String userObjectClass, groupObjectClass;

	private final boolean readOnly;
	private final boolean disabled;
	private final String uri;

	private String forcedPassword;

	private final boolean scoped;

	private String memberAttributeId = "member";
	private List<String> credentialAttributeIds = Arrays
			.asList(new String[] { LdapAttrs.userPassword.name(), LdapAttrs.authPassword.name() });

	private WorkControl transactionControl;
	private WorkingCopyXaResource<LdapEntryWorkingCopy> xaResource = new WorkingCopyXaResource<>(this);

	public AbstractLdapDirectory(URI uriArg, Dictionary<String, ?> props, boolean scoped) {
		this.properties = new Hashtable<String, Object>();
		for (Enumeration<String> keys = props.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement();
			properties.put(key, props.get(key));
		}
		baseDn = toLdapName(DirectoryConf.baseDn.getValue(properties));
		this.scoped = scoped;

		if (uriArg != null) {
			uri = uriArg.toString();
			// uri from properties is ignored
		} else {
			String uriStr = DirectoryConf.uri.getValue(properties);
			if (uriStr == null)
				uri = null;
			else
				uri = uriStr;
		}

		forcedPassword = DirectoryConf.forcedPassword.getValue(properties);

		userObjectClass = DirectoryConf.userObjectClass.getValue(properties);
		String userBase = DirectoryConf.userBase.getValue(properties);
		groupObjectClass = DirectoryConf.groupObjectClass.getValue(properties);
		String groupBase = DirectoryConf.groupBase.getValue(properties);
		String systemRoleBase = DirectoryConf.systemRoleBase.getValue(properties);
		try {
//			baseDn = new LdapName(UserAdminConf.baseDn.getValue(properties));
			userBaseRdn = new Rdn(userBase);
//			userBaseDn = new LdapName(userBase + "," + baseDn);
			groupBaseRdn = new Rdn(groupBase);
//			groupBaseDn = new LdapName(groupBase + "," + baseDn);
			systemRoleBaseRdn = new Rdn(systemRoleBase);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Badly formated base DN " + DirectoryConf.baseDn.getValue(properties),
					e);
		}

		// read only
		String readOnlyStr = DirectoryConf.readOnly.getValue(properties);
		if (readOnlyStr == null) {
			readOnly = readOnlyDefault(uri);
			properties.put(DirectoryConf.readOnly.name(), Boolean.toString(readOnly));
		} else
			readOnly = Boolean.parseBoolean(readOnlyStr);

		// disabled
		String disabledStr = DirectoryConf.disabled.getValue(properties);
		if (disabledStr != null)
			disabled = Boolean.parseBoolean(disabledStr);
		else
			disabled = false;
	}

	/*
	 * ABSTRACT METHODS
	 */

	public abstract HierarchyUnit doGetHierarchyUnit(LdapName dn);

	public abstract Iterable<HierarchyUnit> doGetDirectHierarchyUnits(LdapName searchBase, boolean functionalOnly);

	protected abstract Boolean daoHasEntry(LdapName dn);

	protected abstract LdapEntry daoGetEntry(LdapName key) throws NameNotFoundException;

	protected abstract List<LdapEntry> doGetEntries(LdapName searchBase, Filter f, boolean deep);

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

	@Override
	public LdapEntryWorkingCopy newWorkingCopy() {
		return new LdapEntryWorkingCopy();
	}

	/*
	 * HIERARCHY
	 */
	@Override
	public HierarchyUnit getHierarchyUnit(String path) {
		LdapName dn = pathToName(path);
		return doGetHierarchyUnit(dn);
	}

	@Override
	public Iterable<HierarchyUnit> getDirectHierarchyUnits(boolean functionalOnly) {
		return doGetDirectHierarchyUnits(baseDn, functionalOnly);
	}

	/*
	 * PATHS
	 */

	@Override
	public String getContext() {
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
			for (String segment : segments) {
				// TODO make attr names configurable ?
				String attr = LdapAttrs.ou.name();
				if (parentRdn != null) {
					if (getUserBaseRdn().equals(parentRdn))
						attr = LdapAttrs.uid.name();
					else if (getGroupBaseRdn().equals(parentRdn))
						attr = LdapAttrs.cn.name();
					else if (getSystemRoleBaseRdn().equals(parentRdn))
						attr = LdapAttrs.cn.name();
				}
				Rdn rdn = new Rdn(attr, segment);
				name.add(rdn);
				parentRdn = rdn;
			}
			return name;
		} catch (InvalidNameException e) {
			throw new IllegalStateException("Cannot get role " + path, e);
		}

	}

	/*
	 * UTILITIES
	 */

	protected static boolean hasObjectClass(Attributes attrs, LdapObjs objectClass) {
		try {
			Attribute attr = attrs.get(LdapAttrs.objectClass.name());
			NamingEnumeration<?> en = attr.getAll();
			while (en.hasMore()) {
				String v = en.next().toString();
				if (v.equalsIgnoreCase(objectClass.name()))
					return true;

			}
			return false;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot search for objectClass " + objectClass.name(), e);
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
	 * ACCESSORS
	 */
	@Override
	public Optional<String> getRealm() {
		Object realm = getProperties().get(DirectoryConf.realm.name());
		if (realm == null)
			return Optional.empty();
		return Optional.of(realm.toString());
	}

	protected LdapName getBaseDn() {
		return (LdapName) baseDn.clone();
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean isDisabled() {
		return disabled;
	}

	/** dn can be null, in that case a default should be returned. */
	public String getUserObjectClass() {
		return userObjectClass;
	}

	public Rdn getUserBaseRdn() {
		return userBaseRdn;
	}

	protected String newUserObjectClass(LdapName dn) {
		return getUserObjectClass();
	}

	public String getGroupObjectClass() {
		return groupObjectClass;
	}

	public Rdn getGroupBaseRdn() {
		return groupBaseRdn;
	}

	public Rdn getSystemRoleBaseRdn() {
		return systemRoleBaseRdn;
	}

	public Dictionary<String, Object> getProperties() {
		return properties;
	}

	public Dictionary<String, Object> cloneProperties() {
		return new Hashtable<>(properties);
	}

	public String getForcedPassword() {
		return forcedPassword;
	}

	public boolean isScoped() {
		return scoped;
	}

	public String getMemberAttributeId() {
		return memberAttributeId;
	}

	public List<String> getCredentialAttributeIds() {
		return credentialAttributeIds;
	}

	protected String getUri() {
		return uri;
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
