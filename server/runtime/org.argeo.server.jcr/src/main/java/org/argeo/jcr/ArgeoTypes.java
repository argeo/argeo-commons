package org.argeo.jcr;

/** JCR types in the http://www.argeo.org/argeo namespace */
public interface ArgeoTypes {
	public final static String ARGEO_LINK = "argeo:link";
	public final static String ARGEO_USER_HOME = "argeo:userHome";
	public final static String ARGEO_USER_PROFILE = "argeo:userProfile";
	public final static String ARGEO_REMOTE_REPOSITORY = "argeo:remoteRepository";
	public final static String ARGEO_PREFERENCE_NODE = "argeo:preferenceNode";

	// tabular
	public final static String ARGEO_TABLE = "argeo:table";
	public final static String ARGEO_COLUMN = "argeo:column";
	public final static String ARGEO_CSV = "argeo:csv";

	// crypto
	public final static String ARGEO_ENCRYPTED = "argeo:encrypted";
	public final static String ARGEO_PBE_SPEC = "argeo:pbeSpec";

}
