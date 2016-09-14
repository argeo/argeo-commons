package org.argeo.node;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

class EnumOCD<T extends Enum<T>> implements ObjectClassDefinition {
	private final Class<T> enumClass;
	private String locale;

	public EnumOCD(Class<T> clazz, String locale) {
		this.enumClass = clazz;
		this.locale = locale;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getID() {
		return enumClass.getName();
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public AttributeDefinition[] getAttributeDefinitions(int filter) {
		EnumSet<T> set = EnumSet.allOf(enumClass);
		List<AttributeDefinition> attrs = new ArrayList<>();
		for (T key : set)
			attrs.add((AttributeDefinition) key);
		return attrs.toArray(new AttributeDefinition[attrs.size()]);
	}

	@Override
	public InputStream getIcon(int size) throws IOException {
		return null;
	}

}
