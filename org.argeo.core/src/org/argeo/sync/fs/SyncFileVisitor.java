package org.argeo.sync.fs;

import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.fs.BasicSyncFileVisitor;

/** Synchronises two directory structures. */
public class SyncFileVisitor extends BasicSyncFileVisitor {
	private final static Log log = LogFactory.getLog(SyncFileVisitor.class);

	public SyncFileVisitor(Path sourceBasePath, Path targetBasePath, boolean delete) {
		super(sourceBasePath, targetBasePath, delete);
	}

	@Override
	protected void error(Object obj, Throwable e) {
		log.error(obj, e);
	}

	@Override
	protected boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}

	@Override
	protected void debug(Object obj) {
		log.debug(obj);
	}
}
