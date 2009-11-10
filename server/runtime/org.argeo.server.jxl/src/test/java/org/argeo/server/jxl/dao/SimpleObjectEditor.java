package org.argeo.server.jxl.dao;

import java.beans.PropertyEditorSupport;

public class SimpleObjectEditor extends PropertyEditorSupport {

	@Override
	public String getAsText() {
		return ((SimpleObject) getValue()).getString();
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		SimpleObject obj = new SimpleObject();
		obj.setString(text);
		setValue(obj);
	}

}
