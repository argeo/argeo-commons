package org.argeo.api.acr.search;

import javax.xml.namespace.QName;

/** Whether this property is greater than this value or equal. */
public class Gte extends PropertyValueContraint {
	public Gte(QName prop, Object value) {
		super(prop, value);
	}
}
