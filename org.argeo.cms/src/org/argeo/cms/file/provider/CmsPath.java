package org.argeo.cms.file.provider;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.fs.AbstractFsPath;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;

public class CmsPath extends AbstractFsPath<CmsFileSystem, CmsFileStore> {
	final static String SEPARATOR = "/";

	// lazy loaded
	private ProvidedContent content;

	ProvidedContent getContent() {
		if (content == null) {
			content = getFileSystem().getContent(toString());
		}
		return content;
	}

	CmsPath(CmsFileSystem fileSystem, Content content) {
		this(fileSystem, content.getPath());
		this.content = (ProvidedContent) content;
	}

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

	ProvidedSession getContentSession() {
		return getFileSystem().getContentSession();
	}

}
