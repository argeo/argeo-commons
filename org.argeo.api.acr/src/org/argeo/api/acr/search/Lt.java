package org.argeo.api.acr.search;

import javax.xml.namespace.QName;

/** Whether this property is strictly less than this value. */
public class Lt extends PropertyValueContraint {
	public Lt(QName prop, Object value) {
		super(prop, value);
	}
}
