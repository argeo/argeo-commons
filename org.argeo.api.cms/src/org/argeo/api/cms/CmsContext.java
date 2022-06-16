package org.argeo.api.cms;

import java.util.List;
import java.util.Locale;

import javax.security.auth.Subject;

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

	Locale getDefaultLocale();

	List<Locale> getLocales();

	Long getAvailableSince();

	/** Mark this group as a workgroup */
	void createWorkgroup(String groupDn);

	/** Get the CMS session of this subject. */
	CmsSession getCmsSession(Subject subject);
	
	CmsState getCmsState();
}
