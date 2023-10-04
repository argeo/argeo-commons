package org.argeo.api.acr.search;

import javax.xml.namespace.QName;

/** Whether this property is less than this value or equal. */
public class Lte extends PropertyValueContraint {
	public Lte(QName prop, Object value) {
		super(prop, value);
	}
}
