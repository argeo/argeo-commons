package org.argeo.api.cms;

/**
 * A logical view on this CMS instance, independently of a particular launch or
 * deployment.
 */
public interface CmsContext {
	/**
	 * To be used as an identifier of a workgroup, typically as a value for the
	 * 'businessCategory' attribute in LDAP.
	 */
	public final static String WORKGROUP = "workgroup";

	/** Mark this group as a workgroup */
	void createWorkgroup(String groupDn);
}
