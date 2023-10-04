package org.argeo.api.acr.search;

import javax.xml.namespace.QName;

/** Whether this property equals this value. */
public class Eq extends PropertyValueContraint {
	public Eq(QName prop, Object value) {
		super(prop, value);
	}
}
