package org.argeo.api.gcr;

import java.util.UUID;

/** Standard names. */
public enum CrName implements ContentName {
	/*
	 * TYPES
	 */
	COLLECTION("collection"), // a collection type

	/*
	 * ATTRIBUTES
	 */
	UUID("uuid"), // the UUID of a content
	//
	;

	private String name;
	private UUID uuid;

	CrName(String name) {
		this.name = name;
		this.uuid = ContentNamespace.CR_NS.nameUuid(name);
	}

	@Override
	public UUID getUuid() {
		return uuid;
	}

	@Override
	public ContentNamespace getNamespace() {
		return ContentNamespace.CR_NS;
	}

	@Override
	public String getName() {
		return name;
	}

}
