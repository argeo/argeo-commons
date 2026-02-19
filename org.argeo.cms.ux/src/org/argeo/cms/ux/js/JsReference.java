package org.argeo.cms.ux.js;

import java.util.function.Supplier;

/** Plain JS reference, to be directly serialised. */
public class JsReference implements Supplier<String> {
	private final String reference;

	public JsReference(String reference) {
		this.reference = reference;
	}

	public String get() {
		return reference;
	}

}
