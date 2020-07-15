package org.argeo.api;

/** JCR types in the http://www.argeo.org/node namespace */
@Deprecated
public interface NodeNames {
	String LDAP_UID = "ldap:"+NodeConstants.UID;
	String LDAP_CN = "ldap:"+NodeConstants.CN;
}
