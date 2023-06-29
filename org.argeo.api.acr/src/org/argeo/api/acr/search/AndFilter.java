package org.argeo.api.acr.search;

/** AND filter based on the intersection composition. */
public class AndFilter extends ContentFilter<Intersection> {

	public AndFilter() {
		super(Intersection.class);
	}

}
