package org.argeo.osgi.metatype;

import org.osgi.service.metatype.AttributeDefinition;

public interface EnumAD extends AttributeDefinition {
	String name();

	default Object getDefault() {
		return null;
	}

	@Override
	default String getName() {
		return name();
	}

	@Override
	default String getID() {
		return getClass().getName() + "." + name();
	}

	@Override
	default String getDescription() {
		return null;
	}

	@Override
	default int getCardinality() {
		return 0;
	}

	@Override
	default int getType() {
		return STRING;
	}

	@Override
	default String[] getOptionValues() {
		return null;
	}

	@Override
	default String[] getOptionLabels() {
		return null;
	}

	@Override
	default String validate(String value) {
		return null;
	}

	@Override
	default String[] getDefaultValue() {
		Object value = getDefault();
		if (value == null)
			return null;
		return new String[] { value.toString() };
	}
}
