/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
