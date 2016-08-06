package org.argeo.node;

/** JCR repository configuration */
public enum RepoConf {
	/** Repository type */
	type("localfs"),
	/** Default workspace */
	defaultWorkspace("main"),
	/** Database URL */
	dburl(null),
	/** Database user */
	dbuser(null),
	/** Database password */
	dbpassword(null),

	/** The identifier (can be an URL locating the repo) */
	uri(null),

	//
	// JACKRABBIT SPECIFIC
	//
	/** Maximum database pool size */
	maxPoolSize(10),
	/** Maximum cache size in MB */
	maxCacheMB(null),
	/** Bundle cache size in MB */
	bundleCacheMB(8),
	/** Extractor pool size */
	extractorPoolSize(0),
	/** Search cache size */
	searchCacheSize(1000),
	/** Max volatile index size */
	maxVolatileIndexSize(1048576);

	/** The default value. */
	private Object def;

	RepoConf(Object def) {
		this.def = def;
	}

	public Object getDefault() {
		return def;
	}
}
