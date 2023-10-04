package org.argeo.api.acr.search;

import javax.xml.namespace.QName;

import org.argeo.api.acr.QNamed;

/** Whether the content is all these content classes. */
public class IsContentClass implements Constraint {
	final QName[] contentClasses;

	public IsContentClass(QName[] contentClasses) {
		this.contentClasses = contentClasses;
	}

	public IsContentClass(QNamed[] contentClasses) {
		this.contentClasses = new QName[contentClasses.length];
		for (int i = 0; i < contentClasses.length; i++)
			this.contentClasses[i] = contentClasses[i].qName();
	}

	public QName[] getContentClasses() {
		return contentClasses;
	}

}
