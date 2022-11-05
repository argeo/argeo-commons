package org.argeo.api.acr.ldap;

interface NodeOID {
	String BASE = "1.3.6.1.4.1" + ".48308" + ".1";

	// uuidgen --md5 --namespace @oid --name 1.3.6.1.4.1.48308
	String BASE_UUID_V3 = "6869e86b-96b7-3d49-b6ab-ffffc5ad95fb";
	
	// uuidgen --sha1 --namespace @oid --name 1.3.6.1.4.1.48308
	String BASE_UUID_V5 = "58873947-460c-59a6-a7b4-28a97def5f27";
	
	// ATTRIBUTE TYPES
	String ATTRIBUTE_TYPES = BASE + ".4";

	// OBJECT CLASSES
	String OBJECT_CLASSES = BASE + ".6";
}
