package org.argeo.cms.internal.kernel;

import org.argeo.osgi.metatype.EnumAD;
import org.argeo.osgi.metatype.EnumOCD;

/** JCR repository configuration */
enum RepoConf implements EnumAD {
	/** Repository type */
	type("localfs"),
	/** Default workspace */
	@Deprecated defaultWorkspace("main"),
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
	@Deprecated maxCacheMB(null),
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
	private String oid;

	RepoConf(String oid, Object def) {
		this.oid = oid;
		this.def = def;
	}

	RepoConf(Object def) {
		this.def = def;
	}

	public Object getDefault() {
		return def;
	}

	@Override
	public String getID() {
		if (oid != null)
			return oid;
		return EnumAD.super.getID();
	}

	public static class OCD extends EnumOCD<RepoConf> {
		public OCD(String locale) {
			super(RepoConf.class, locale);
		}
	}

}
