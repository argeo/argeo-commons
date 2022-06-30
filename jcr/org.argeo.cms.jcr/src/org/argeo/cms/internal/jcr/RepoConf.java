package org.argeo.cms.internal.jcr;

import org.argeo.api.cms.CmsConstants;

/** JCR repository configuration */
public enum RepoConf {
	/** Repository type */
	type("h2"),
	/** Default workspace */
	defaultWorkspace(CmsConstants.SYS_WORKSPACE),
	/** Database URL */
	dburl(null),
	/** Database user */
	dbuser(null),
	/** Database password */
	dbpassword(null),

	/** The identifier (can be an URL locating the repo) */
	labeledUri(null),
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
	maxVolatileIndexSize(1048576),
	/** Cluster id (if appropriate configuration) */
	clusterId("default"),
	/** Indexes base path */
	indexesBase(null);

	/** The default value. */
	private Object def;

	RepoConf(String oid, Object def) {
		this.def = def;
	}

	RepoConf(Object def) {
		this.def = def;
	}

	public Object getDefault() {
		return def;
	}

}
