package org.argeo.cms.internal.jcr;

/** Pre-defined Jackrabbit repository configurations. */
enum JackrabbitType {
	/** Local file system */
	localfs,
	/** Embedded Java H2 database */
	h2,
	/** Embedded Java H2 database in PostgreSQL compatibility mode */
	h2_postgresql,
	/** PostgreSQL */
	postgresql,
	/** PostgreSQL with datastore */
	postgresql_ds,
	/** PostgreSQL with cluster */
	postgresql_cluster,
	/** PostgreSQL with cluster and datastore */
	postgresql_cluster_ds,
	/** Memory */
	memory;
}
