package org.argeo.util;

import java.beans.PropertyEditorSupport;

public class ThroughputEditor extends PropertyEditorSupport {

	@Override
	public String getAsText() {
		return getValue().toString();
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(new Throughput(text));
	}

}
