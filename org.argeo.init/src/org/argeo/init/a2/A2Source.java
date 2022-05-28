package org.argeo.init.a2;

import java.net.URI;

/** A provisioning source in A2 format. */
public interface A2Source extends ProvisioningSource {
	final static String SCHEME_A2 = "a2";
	final static String DEFAULT_A2_URI = SCHEME_A2 + ":///";

	URI getUri();
}
