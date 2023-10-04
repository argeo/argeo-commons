package org.argeo.api.acr.search;

import javax.xml.namespace.QName;

/** Whether this property equals this value. */
public class Like extends PropertyValueContraint {
	public Like(QName prop, String pattern) {
		super(prop, pattern);
	}
}
