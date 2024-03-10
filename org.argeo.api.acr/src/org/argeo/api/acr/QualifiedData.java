package org.argeo.api.acr;

import javax.xml.namespace.QName;

/** A {@link StructuredData} whose attributes have qualified keys. */
public interface QualifiedData<CHILD extends QualifiedData<CHILD>> extends StructuredData<QName, Object, CHILD> {

}
