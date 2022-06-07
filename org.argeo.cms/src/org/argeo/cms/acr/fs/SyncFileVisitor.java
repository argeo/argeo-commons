package org.argeo.cms.acr.fs;

import java.nio.file.Path;
import java.util.Objects;

import org.argeo.api.cms.CmsLog;

/** Synchronises two directory structures. */
public class SyncFileVisitor extends BasicSyncFileVisitor {
	private final static CmsLog log = CmsLog.getLog(SyncFileVisitor.class);

	public SyncFileVisitor(Path sourceBasePath, Path targetBasePath, boolean delete, boolean recursive) {
		super(sourceBasePath, targetBasePath, delete, recursive);
	}

	@Override
	protected void error(Object obj, Throwable e) {
		log.error(Objects.toString(obj), e);
	}

	@Override
	protected boolean isTraceEnabled() {
		return log.isTraceEnabled();
	}

	@Override
	protected void trace(Object obj) {
		log.error(Objects.toString(obj));
	}
}
