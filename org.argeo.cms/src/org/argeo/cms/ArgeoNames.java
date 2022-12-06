package org.argeo.cms;

/** JCR names in the http://www.argeo.org/argeo namespace */
@Deprecated
public interface ArgeoNames {
	public final static String ARGEO_NAMESPACE = "http://www.argeo.org/ns/argeo";

	public final static String ARGEO_URI = "argeo:uri";
	public final static String ARGEO_USER_ID = "argeo:userID";

	public final static String ARGEO_REMOTE = "argeo:remote";
	public final static String ARGEO_PASSWORD = "argeo:password";
	// tabular
	public final static String ARGEO_IS_KEY = "argeo:isKey";

	// crypto
	public final static String ARGEO_IV = "argeo:iv";
	public final static String ARGEO_SECRET_KEY_FACTORY = "argeo:secretKeyFactory";
	public final static String ARGEO_SALT = "argeo:salt";
	public final static String ARGEO_ITERATION_COUNT = "argeo:iterationCount";
	public final static String ARGEO_KEY_LENGTH = "argeo:keyLength";
	public final static String ARGEO_SECRET_KEY_ENCRYPTION = "argeo:secretKeyEncryption";
	public final static String ARGEO_CIPHER = "argeo:cipher";
	public final static String ARGEO_KEYRING = "argeo:keyring";
}
