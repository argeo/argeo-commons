package org.argeo.cms.internal.runtime;

import org.argeo.api.cms.CmsConstants;

/** Internal CMS constants. */
interface KernelConstants {
	// Directories
	String DIR_PRIVATE = "private";

	// Files
	/**
	 * Kerberos 5 keytab which will be common to all IPA-enabled children
	 * frameworks.
	 */
	String PROP_ARGEO_NODE_KRB5_KEYTAB = "argeo.node.krb5.keytab";
	String NODE_KEY_TAB_PATH = DIR_PRIVATE + "/krb5.keytab";
	String NODE_SSHD_AUTHORIZED_KEYS_PATH = DIR_PRIVATE + "/authorized_keys";

	// Security
	String JAAS_CONFIG = "/org/argeo/cms/internal/runtime/jaas.cfg";
	String JAAS_CONFIG_IPA = "/org/argeo/cms/internal/runtime/jaas-ipa.cfg";

	// KERBEROS
	String DEFAULT_KERBEROS_SERVICE = "HTTP";

	String DEFAULT_KEYSTORE_PATH = DIR_PRIVATE + '/' + CmsConstants.NODE + ".p12";

	String DEFAULT_TRUSTSTORE_PATH = DIR_PRIVATE + "/trusted.p12";

	String DEFAULT_PEM_KEY_PATH = DIR_PRIVATE + '/' + CmsConstants.NODE + ".key";

	String DEFAULT_PEM_CERT_PATH = DIR_PRIVATE + '/' + CmsConstants.NODE + ".crt";

	String IPA_PEM_CA_CERT_PATH = "/etc/ipa/ca.crt";

	String DEFAULT_KEYSTORE_PASSWORD = "changeit";

	String PKCS12 = "PKCS12";

	// HTTP client
	// String COOKIE_POLICY_BROWSER_COMPATIBILITY = "compatibility";

}
