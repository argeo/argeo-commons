package org.argeo.api.cli;

import org.argeo.api.acr.CrAttributeType;

/** An option which may possibly hold one or multiple value. */
public interface ValuedOpt {
	default boolean hasValue() {
		return false;
	}

	default CrAttributeType type() {
		return CrAttributeType.STRING;
	}

	default <T> T defaultValue() {
		return null;
	}

	default boolean isMultiple() {
		return false;
	}
}
