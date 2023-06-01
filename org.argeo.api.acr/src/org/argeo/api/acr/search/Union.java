package org.argeo.api.acr.search;

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
