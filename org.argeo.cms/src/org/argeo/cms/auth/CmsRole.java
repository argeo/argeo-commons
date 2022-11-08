package org.argeo.cms.auth;

import javax.xml.namespace.QName;

import org.argeo.api.acr.ArgeoNamespace;
import org.argeo.api.acr.ContentName;

/** Standard CMS system roles. */
public enum CmsRole implements SystemRole {
	userAdmin, //
	groupAdmin, //
	//
	;

	private final static String QUALIFIER = "cms.";

	private final ContentName name;

	CmsRole() {
		name = new ContentName(ArgeoNamespace.ROLE_NAMESPACE_URI, QUALIFIER + name());
	}

	@Override
	public QName getName() {
		return name;
	}

	@Override
	public String toString() {
		return name.toPrefixedString();
	}
}
