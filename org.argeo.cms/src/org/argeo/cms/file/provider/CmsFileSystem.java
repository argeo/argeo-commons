package org.argeo.cms.file.provider;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;

import org.argeo.api.acr.fs.AbstractFsSystem;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.CmsSession;
import org.argeo.cms.acr.ContentUtils;

public class CmsFileSystem extends AbstractFsSystem<CmsFileStore> {
	private final CmsFileSystemProvider provider;
//	private final ProvidedRepository contentRepository;
	private final CmsSession cmsSession;
	private final ProvidedSession contentSession;

	private final CmsPath rootPath;
	private final CmsFileStore baseFileStore;

	public CmsFileSystem(CmsFileSystemProvider provider, ProvidedRepository contentRepository, CmsSession cmsSession) {
		this.provider = provider;
//		this.contentRepository = contentRepository;
		this.cmsSession = cmsSession;
		this.contentSession = (ProvidedSession) ContentUtils.openSession(contentRepository, cmsSession);

		rootPath = new CmsPath(this, ProvidedContent.ROOT_PATH);
		baseFileStore = new CmsFileStore(rootPath.getContent().getProvider());
	}

	@Override
	public CmsFileStore getBaseFileStore() {
		return baseFileStore;
	}

	@Override
	public CmsFileStore getFileStore(String path) {
		ProvidedContent c = (ProvidedContent) contentSession.get(path);
		return new CmsFileStore(c.getProvider());
	}

	@Override
	public FileSystemProvider provider() {
		return provider;
	}

	@Override
	public void close() throws IOException {
		// TODO close content session?
		provider.close(this);
	}

	@Override
	public boolean isOpen() {
		// TODO check provider
		return true;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public String getSeparator() {
		return CmsPath.SEPARATOR;
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		return Collections.singleton(rootPath);
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		// TODO return all mount points
		return Collections.singleton(baseFileStore);
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return Collections.singleton(ContentAttributeView.NAME);
	}

	@Override
	public Path getPath(String first, String... more) {
		StringBuilder sb = new StringBuilder(first);
		// TODO Make it more robust
		for (String part : more)
			sb.append('/').append(part);
		return new CmsPath(this, sb.toString());
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		return null;
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		return null;
	}

	@Override
	public WatchService newWatchService() throws IOException {
		return null;
	}

	/*
	 * ACR
	 */

	ProvidedContent getContent(String acrPath) {
		return (ProvidedContent) contentSession.get(acrPath);
	}

	ProvidedSession getContentSession() {
		return contentSession;
	}

	/*
	 * CMS
	 */

	CmsSession getCmsSession() {
		return cmsSession;
	}

}
