package org.argeo.api.cli;

import static org.argeo.api.acr.CrAttributeType.BOOLEAN;

import org.argeo.api.acr.CrAttributeType;

/** An option which may possibly hold a single or multiple values. */
public interface ValuedOpt {
	/** Whether this option can have a non-boolean value. */
	static boolean isFlag(ValuedOpt opt) {
		return opt.type() == BOOLEAN && !opt.isMultiple();
	}

	/** The type of this option value. */
	default CrAttributeType type() {
		return BOOLEAN;
	}

	/** The default value or <code>null</code> if none is available. */
	default Object defaultValue() {
		if (BOOLEAN == type())
			return Boolean.FALSE;
		return null;
	}

	/** Whether this is a multi-valued option. */
	default boolean isMultiple() {
		return false;
	}
}
