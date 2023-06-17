package org.argeo.api.acr.search;

/** A composition which is the intersection of sets (AND). */
class Intersection implements Composition {
	ContentFilter<Intersection> filter;

	@SuppressWarnings("unchecked")
	public Intersection(ContentFilter<?> filter) {
		this.filter = (ContentFilter<Intersection>) filter;
	}

	public ContentFilter<Intersection> and() {
		return filter;
	}

}
