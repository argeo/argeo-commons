package org.argeo.node;

interface NodeOID {
	String BASE = "1.3.6.1.4.1" + ".48308" + ".1";

	// ATTRIBUTE TYPES
	String ATTRIBUTE_TYPES = BASE + ".4";
	String URI = ATTRIBUTE_TYPES + ".1";
	String HTTP_PORT = ATTRIBUTE_TYPES + ".2";
	String HTTPS_PORT = ATTRIBUTE_TYPES + ".3";

	// OBJECT CLASSES
	String OBJECT_CLASSES = BASE + ".6";
	String JCR_REPOSITORY = OBJECT_CLASSES + ".1";

	// EXTERNAL
	String LABELED_URI = "1.3.6.1.4.1.250.1.57";
}
