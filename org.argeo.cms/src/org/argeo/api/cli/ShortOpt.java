package org.argeo.api.cli;

public interface ShortOpt {
	default char shortOpt() {
		return Character.MIN_VALUE; // NULL
	}
}
