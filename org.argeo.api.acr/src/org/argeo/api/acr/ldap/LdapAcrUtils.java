package org.argeo.api.acr.ldap;

import java.util.Locale;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;

/** Utilities around ACR and LDAP conventions. */
public class LdapAcrUtils {

	/** singleton */
	private LdapAcrUtils() {
	}

	public static Object getLocalized(Content content, QName key, Locale locale) {
		if (locale == null)
			throw new IllegalArgumentException("A locale must be specified");
		Object value = null;
		if (locale.getCountry() != null && !locale.getCountry().equals(""))
			value = content.get(new ContentName(key.getNamespaceURI(),
					key.getLocalPart() + ";lang-" + locale.getLanguage() + "-" + locale.getCountry()));
		if (value == null)
			value = content
					.get(new ContentName(key.getNamespaceURI(), key.getLocalPart() + ";lang-" + locale.getLanguage()));
		if (value == null)
			value = content.get(key);
		return value;
	}
}
