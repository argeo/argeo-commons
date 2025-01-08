package org.argeo.api.cli;

public interface MultipleOpt {
	default boolean isMultiple() {
		return false;
	}
}
