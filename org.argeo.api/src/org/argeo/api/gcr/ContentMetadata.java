package org.argeo.api.gcr;

import java.io.Serializable;
import java.util.Set;

public interface ContentMetadata extends Serializable {
	Set<String> getKnownKeys();

	Set<String> getTypes();

	/** Whether that content can have unknown keys. */
	default boolean isStructured() {
		return false;
	}
}
