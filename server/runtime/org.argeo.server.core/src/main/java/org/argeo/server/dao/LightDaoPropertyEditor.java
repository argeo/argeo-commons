package org.argeo.server.dao;

import java.beans.PropertyEditorSupport;

import org.argeo.ArgeoException;

public class LightDaoPropertyEditor extends PropertyEditorSupport implements
		LightDaoAware {
	private LightDaoSupport lightDaoSupport;

	private Class<?> targetClass;
	/** Can be null */
	private String businessIdField;

	@Override
	public String getAsText() {
		return getValue().toString();
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (targetClass == null)
			throw new ArgeoException("Target class cannot be null");

		if (businessIdField != null)
			setValue(lightDaoSupport.getByField(targetClass, businessIdField,
					text));
		else
			setValue(lightDaoSupport.getByKey(targetClass, text));
	}

	public void setLightDaoSupport(LightDaoSupport lightDaoSupport) {
		this.lightDaoSupport = lightDaoSupport;
	}

	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	public void setBusinessIdField(String businessIdField) {
		this.businessIdField = businessIdField;
	}

}
