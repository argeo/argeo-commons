package org.argeo.cms.auth;

import javax.xml.namespace.QName;

import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.CrName;

/** Standard CMS system roles. */
public enum CmsRole implements SystemRole {
	userAdmin, //
	groupAdmin;

	private final static String QUALIFIER = "cms.";

	private final ContentName name;

	CmsRole() {
		name = new ContentName(CrName.ROLE_NAMESPACE_URI, QUALIFIER + name());
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
