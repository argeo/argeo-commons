package org.argeo.cms.acr.fs;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.Objects;

/** Synchronises two directory structures. */
public class SyncFileVisitor extends BasicSyncFileVisitor {
	private final static Logger logger = System.getLogger(SyncFileVisitor.class.getName());

	public SyncFileVisitor(Path sourceBasePath, Path targetBasePath, boolean delete, boolean recursive) {
		super(sourceBasePath, targetBasePath, delete, recursive);
	}

	@Override
	protected void error(Object obj, Throwable e) {
		logger.log(Level.ERROR, Objects.toString(obj), e);
	}

	@Override
	protected boolean isTraceEnabled() {
		return logger.isLoggable(Level.TRACE);
	}

	@Override
	protected void trace(Object obj) {
		logger.log(Level.TRACE, Objects.toString(obj));
	}
}
