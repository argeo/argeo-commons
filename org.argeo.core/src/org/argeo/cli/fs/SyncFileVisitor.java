package org.argeo.cli.fs;

import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.fs.BasicSyncFileVisitor;

/** Synchronises two directory structures. */
public class SyncFileVisitor extends BasicSyncFileVisitor {
	private final static Log log = LogFactory.getLog(SyncFileVisitor.class);

	public SyncFileVisitor(Path sourceBasePath, Path targetBasePath, boolean delete, boolean recursive) {
		super(sourceBasePath, targetBasePath, delete, recursive);
	}

	@Override
	protected void error(Object obj, Throwable e) {
		log.error(obj, e);
	}

	@Override
	protected boolean isTraceEnabled() {
		return log.isTraceEnabled();
	}

	@Override
	protected void trace(Object obj) {
		log.trace(obj);
	}
}
