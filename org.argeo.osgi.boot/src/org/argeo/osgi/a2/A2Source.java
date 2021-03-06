package org.argeo.osgi.a2;

/** A provisioning source in A2 format. */
public interface A2Source extends ProvisioningSource {
	final static String SCHEME_A2 = "a2";
	final static String DEFAULT_A2_URI = SCHEME_A2 + ":///";
}
