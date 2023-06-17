package org.argeo.api.acr.search;

/** A composition which is the union of sets (OR). */
class Union implements Composition {
	ContentFilter<Union> filter;

	@SuppressWarnings("unchecked")
	public Union(ContentFilter<?> filter) {
		this.filter = (ContentFilter<Union>) filter;
	}

	public ContentFilter<Union> or() {
		return filter;
	}

}
