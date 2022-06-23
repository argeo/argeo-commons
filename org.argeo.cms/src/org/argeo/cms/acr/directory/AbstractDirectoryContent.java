package org.argeo.cms.acr.directory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.namespace.QName;

import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.acr.AbstractContent;
import org.argeo.util.naming.LdapAttrs;
import org.argeo.util.naming.LdapObjs;

abstract class AbstractDirectoryContent extends AbstractContent {
	protected final DirectoryContentProvider provider;

	public AbstractDirectoryContent(ProvidedSession session, DirectoryContentProvider provider) {
		super(session);
		this.provider = provider;
	}

	abstract Dictionary<String, Object> doGetProperties();

	@SuppressWarnings("unchecked")
	@Override
	public <A> Optional<A> get(QName key, Class<A> clss) {
		String attrName = key.getLocalPart();
		Object value = doGetProperties().get(attrName);
		if (value == null)
			return Optional.empty();
		// TODO deal with type and multiple
		return Optional.of((A) value);
	}

	@Override
	protected Iterable<QName> keys() {
		Dictionary<String, Object> properties = doGetProperties();
		Set<QName> keys = new TreeSet<>(NamespaceUtils.QNAME_COMPARATOR);
		keys: for (Enumeration<String> it = properties.keys(); it.hasMoreElements();) {
			String key = it.nextElement();
			if (key.equalsIgnoreCase(LdapAttrs.objectClass.name()))
				continue keys;
			if (key.equalsIgnoreCase(LdapAttrs.objectClasses.name()))
				continue keys;
			ContentName name = new ContentName(CrName.LDAP_NAMESPACE_URI, key, provider);
			keys.add(name);
		}
		return keys;
	}

	@Override
	public List<QName> getContentClasses() {
		Dictionary<String, Object> properties = doGetProperties();
		List<QName> contentClasses = new ArrayList<>();
		String objectClass = properties.get(LdapAttrs.objectClass.name()).toString();
		contentClasses.add(new ContentName(CrName.LDAP_NAMESPACE_URI, objectClass, provider));

		String[] objectClasses = properties.get(LdapAttrs.objectClasses.name()).toString().split("\\n");
		objectClasses: for (String oc : objectClasses) {
			if (LdapObjs.top.name().equalsIgnoreCase(oc))
				continue objectClasses;
			if (objectClass.equalsIgnoreCase(oc))
				continue objectClasses;
			contentClasses.add(new ContentName(CrName.LDAP_NAMESPACE_URI, oc, provider));
		}
		return contentClasses;
	}

	@Override
	public Object put(QName key, Object value) {
		Object previous = get(key);
		// TODO deal with typing
		doGetProperties().put(key.getLocalPart(), value);
		return previous;
	}

	@Override
	protected void removeAttr(QName key) {
		doGetProperties().remove(key.getLocalPart());
	}

	@Override
	public ContentProvider getProvider() {
		return provider;
	}


}
