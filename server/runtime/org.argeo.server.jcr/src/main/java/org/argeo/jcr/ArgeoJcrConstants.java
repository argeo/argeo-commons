package org.argeo.jcr;

/** JCR related constants */
public interface ArgeoJcrConstants {
	public final static String ARGEO_BASE_PATH = "/argeo:system";
	public final static String DATA_MODELS_BASE_PATH = ARGEO_BASE_PATH
			+ "/argeo:dataModels";
	/** The home base path. Not yet configurable */
	public final static String DEFAULT_HOME_BASE_PATH = "/argeo:home";

	// parameters (typically for call to a RepositoryFactory)
	public final static String JCR_REPOSITORY_ALIAS = "argeo.jcr.repository.alias";
	public final static String JCR_REPOSITORY_URI = "argeo.jcr.repository.uri";

	// standard aliases
	public final static String ALIAS_NODE = "node";

}
