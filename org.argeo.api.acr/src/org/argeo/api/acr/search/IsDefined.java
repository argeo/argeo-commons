package org.argeo.api.acr.search;

import javax.xml.namespace.QName;

/** Whether this property is defined. */
public class IsDefined implements Constraint {
	final QName prop;

	public IsDefined(QName prop) {
		this.prop = prop;
	}

	public QName getProp() {
		return prop;
	}
}
