package org.argeo.cms.internal.runtime;

/** Internal CMS constants. */
public interface KernelConstants {
	// Directories
	String DIR_PRIVATE = "private";

	// Files
	String NODE_KEY_TAB_PATH = DIR_PRIVATE + "/krb5.keytab";
	String NODE_SSHD_AUTHORIZED_KEYS_PATH = DIR_PRIVATE + "/authorized_keys";

	// Security
	String JAAS_CONFIG = "/org/argeo/cms/internal/runtime/jaas.cfg";
	String JAAS_CONFIG_IPA = "/org/argeo/cms/internal/runtime/jaas-ipa.cfg";

	// KERBEROS
	String DEFAULT_KERBEROS_SERVICE = "HTTP";

	// HTTP client
	// String COOKIE_POLICY_BROWSER_COMPATIBILITY = "compatibility";

}
