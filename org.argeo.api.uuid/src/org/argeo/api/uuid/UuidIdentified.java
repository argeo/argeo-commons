package org.argeo.api.uuid;

import java.util.UUID;

/**
 * An object identified by a {@link UUID}. Typically used to fasten indexing and
 * comparisons of objects or records. THe method to implement is {@link #uuid()}
 * so that any record with an <code>uuid</code> field can easily be enriched
 * with this interface.
 */
public interface UuidIdentified {
	/** The UUID identifier. */
	UUID uuid();

	/** The UUID identifier, for compatibility with beans accessors. */
	default UUID getUuid() {
		return uuid();
	}

	/**
	 * Helper to implement the equals method of an {@link UuidIdentified}.<br/>
	 * 
	 * <pre>
	 * &#64;Override
	 * public boolean equals(Object o) {
	 * 	return UuidIdentified.equals(this, o);
	 * }
	 * </pre>
	 */
	static boolean equals(UuidIdentified uuidIdentified, Object o) {
		assert uuidIdentified != null;
		if (o == null)
			return false;
		if (uuidIdentified == o)
			return true;
		if (o instanceof UuidIdentified u)
			return uuidIdentified.uuid().equals(u.uuid());
		else
			return false;
	}

	/**
	 * Helper to implement the hash code method of an {@link UuidIdentified}.<br/>
	 * 
	 * <pre>
	 * &#64;Override
	 * public int hashCode() {
	 * 	return UuidIdentified.hashCode(this);
	 * }
	 * </pre>
	 */
	static int hashCode(UuidIdentified uuidIdentified) {
		assert uuidIdentified != null;
		return uuidIdentified.getUuid().hashCode();
	}

}
