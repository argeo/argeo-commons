package org.argeo.cms.acr.directory;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.ldap.LdapName;
import javax.swing.GroupLayout.Group;
import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.acr.AbstractContent;
import org.argeo.osgi.useradmin.LdapNameUtils;
import org.argeo.util.naming.LdapAttrs;
import org.argeo.util.naming.LdapObjs;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

public class RoleContent extends AbstractContent {

	private DirectoryContentProvider provider;
	private HierarchyUnitContent parent;
	private Role role;

	public RoleContent(ProvidedSession session, DirectoryContentProvider provider, HierarchyUnitContent parent,
			Role role) {
		super(session);
		this.provider = provider;
		this.parent = parent;
		this.role = role;
	}

	@Override
	public ContentProvider getProvider() {
		return provider;
	}

	@Override
	public QName getName() {
		LdapName dn = LdapNameUtils.toLdapName(role.getName());
		String name = LdapNameUtils.getLastRdnAsString(dn);
		return new ContentName(name);
	}

	@Override
	public Content getParent() {
		return parent;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> Optional<A> get(QName key, Class<A> clss) {
		String attrName = key.getLocalPart();
		Object value = role.getProperties().get(attrName);
		if (value == null)
			return Optional.empty();
		// TODO deal with type and multiple
		return Optional.of((A) value);
	}

	@Override
	protected Iterable<QName> keys() {
		Set<QName> keys = new TreeSet<>(NamespaceUtils.QNAME_COMPARATOR);
		keys: for (Enumeration<String> it = role.getProperties().keys(); it.hasMoreElements();) {
			String key = it.nextElement();
			if (key.equalsIgnoreCase(LdapAttrs.objectClass.name()))
				continue keys;
			ContentName name = new ContentName(CrName.LDAP_NAMESPACE_URI, key, provider);
			keys.add(name);
		}
		return keys;
	}

	@Override
	public List<QName> getTypes() {
		List<QName> contentClasses = new ArrayList<>();
		keys: for (Enumeration<String> it = role.getProperties().keys(); it.hasMoreElements();) {
			String key = it.nextElement();
			if (key.equalsIgnoreCase(LdapAttrs.objectClass.name())) {
				String[] objectClasses = role.getProperties().get(key).toString().split("\\n");
				objectClasses: for (String objectClass : objectClasses) {
					if (LdapObjs.top.name().equalsIgnoreCase(objectClass))
						continue objectClasses;
					contentClasses.add(new ContentName(CrName.LDAP_NAMESPACE_URI, objectClass, provider));
				}
				break keys;
			}
		}
		return contentClasses;
	}

	@Override
	public Object put(QName key, Object value) {
		Object previous = get(key);
		// TODO deal with typing
		role.getProperties().put(key.getLocalPart(), value);
		return previous;
	}

	@Override
	protected void removeAttr(QName key) {
		role.getProperties().remove(key.getLocalPart());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> A adapt(Class<A> clss) {
		if (clss.equals(Group.class))
			return (A) role;
		else if (clss.equals(User.class))
			return (A) role;
		else if (clss.equals(Role.class))
			return (A) role;
		return super.adapt(clss);
	}

}
