package org.argeo.cms.file.provider;

import org.argeo.api.acr.fs.AbstractFsPath;

public class CmsPath extends AbstractFsPath<CmsFileSystem, CmsFileStore> {

	public CmsPath(CmsFileSystem filesSystem, CmsFileStore fileStore, String[] segments, boolean absolute) {
		super(filesSystem, fileStore, segments, absolute);
	}

	public CmsPath(CmsFileSystem filesSystem, String path) {
		super(filesSystem, path);
	}

	@Override
	protected AbstractFsPath<CmsFileSystem, CmsFileStore> newInstance(String path) {
		return new CmsPath(getFileSystem(), path);
	}

	@Override
	protected AbstractFsPath<CmsFileSystem, CmsFileStore> newInstance(String[] segments, boolean absolute) {
		return new CmsPath(getFileSystem(), getFileStore(), segments, absolute);
	}

}
