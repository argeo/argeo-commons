package org.argeo.api.acr.search;

import javax.xml.namespace.QName;

/** Whether this property is strictly greater than this value. */
public class Gt extends PropertyValueContraint {
	public Gt(QName prop, Object value) {
		super(prop, value);
	}
}
