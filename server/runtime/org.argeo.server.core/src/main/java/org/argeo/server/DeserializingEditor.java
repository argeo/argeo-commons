package org.argeo.server;

import java.beans.PropertyEditorSupport;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;

public class DeserializingEditor extends PropertyEditorSupport {
	private ServerDeserializer deserializer;

	public DeserializingEditor(ServerDeserializer deserializer) {
		super();
		this.deserializer = deserializer;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		StringReader reader = new StringReader(text);
		try {
			setValue(deserializer.deserialize(reader));
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

}
