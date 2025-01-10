package org.argeo.api.cli;

/** Whether this kind of option supports single-character short value. */
public interface ShortOpt {
	default char shortOpt() {
		return Character.MIN_VALUE; // NULL
	}
}
