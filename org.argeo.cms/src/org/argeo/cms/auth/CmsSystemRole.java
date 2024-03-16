package org.argeo.cms.auth;

import javax.xml.namespace.QName;

import org.argeo.api.acr.ArgeoNamespace;
import org.argeo.api.acr.ContentName;
import org.argeo.api.cms.auth.SystemRole;

/** Standard CMS system roles. */
public enum CmsSystemRole implements SystemRole {
	userAdmin, //
	groupAdmin, //
	//
	;

	private final static String QUALIFIER = "cms.";

	private final ContentName name;

	CmsSystemRole() {
		name = new ContentName(ArgeoNamespace.ROLE_NAMESPACE_URI, QUALIFIER + name());
	}

	@Override
	public QName qName() {
		return name;
	}

	@Override
	public String toString() {
		return name.toPrefixedString();
	}
}
