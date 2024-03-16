package org.argeo.api.a2;

import java.net.URI;

/** A provisioning source in A2 format. */
public interface A2Source extends ProvisioningSource {
	/** Use standard a2 protocol, installing from source URL. */
	final static String SCHEME_A2 = "a2";
	/**
	 * Use equinox-specific reference: installation, which does not copy the bundle
	 * content.
	 */
	final static String SCHEME_A2_REFERENCE = "a2+reference";
	final static String DEFAULT_A2_URI = SCHEME_A2 + ":///";
	final static String DEFAULT_A2_REFERENCE_URI = SCHEME_A2_REFERENCE + ":///";

	final static String INCLUDE = "include";
	final static String EXCLUDE = "exclude";

	URI getUri();
}
