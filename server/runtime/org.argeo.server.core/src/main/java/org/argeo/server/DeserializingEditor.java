package org.argeo.server;

import java.beans.PropertyEditorSupport;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;

public class DeserializingEditor extends PropertyEditorSupport {
	private final Deserializer deserializer;
	private final Class<?> targetClass;

	public DeserializingEditor(Deserializer deserializer, Class<?> targetClass) {
		super();
		this.deserializer = deserializer;
		this.targetClass = targetClass;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		StringReader reader = new StringReader(text);
		try {
			setValue(deserializer.deserialize(reader, targetClass));
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

}
