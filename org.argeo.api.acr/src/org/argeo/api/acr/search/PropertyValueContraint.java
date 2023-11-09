package org.argeo.api.acr.search;

import javax.xml.namespace.QName;

/** Whether this property equals this value. */
public abstract class PropertyValueContraint implements Constraint {
	final QName prop;
	final Object value;

	public PropertyValueContraint(QName prop, Object value) {
		this.prop = prop;
		this.value = value;
	}

	public QName getProp() {
		return prop;
	}

	public Object getValue() {
		return value;
	}

}
