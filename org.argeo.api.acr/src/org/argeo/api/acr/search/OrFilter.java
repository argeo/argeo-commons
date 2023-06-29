package org.argeo.api.acr.search;

/** OR filter based on the union composition. */
public class OrFilter extends ContentFilter<Union> {

	public OrFilter() {
		super(Union.class);
	}

}
