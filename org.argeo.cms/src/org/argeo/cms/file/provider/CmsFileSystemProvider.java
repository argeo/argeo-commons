package org.argeo.cms.file.provider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.argeo.api.acr.DName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.CmsSession;
import org.argeo.cms.CurrentUser;

public class CmsFileSystemProvider extends FileSystemProvider {
	private Map<CmsSession, CmsFileSystem> fileSystems = Collections.synchronizedMap(new HashMap<>());

	private ProvidedRepository contentRepository;

	
	public void start() {
		
	}
	
	public void stop() {
		
	}
	
	@Override
	public String getScheme() {
		return "cms";
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		CmsSession cmsSession = CurrentUser.getCmsSession();
		if (cmsSession.isAnonymous()) {
			// TODO deal with anonymous
			return null;
		}
		if (fileSystems.containsKey(cmsSession))
			throw new FileSystemAlreadyExistsException("CMS file system already exists for user " + cmsSession);

		String host = uri.getHost();
		if (host != null && !host.trim().equals("")) {
//				URI repoUri = new URI("http", uri.getUserInfo(), uri.getHost(), uri.getPort(), "/jcr/node", null, null);
			// FIXME deal with remote
			CmsFileSystem fileSystem = null;
			fileSystems.put(cmsSession, fileSystem);
			return fileSystem;
		} else {
			// FIXME send exception if it exists already
			CmsFileSystem fileSystem = new CmsFileSystem(this, contentRepository, cmsSession);
			fileSystems.put(cmsSession, fileSystem);
			cmsSession.addOnCloseCallback((s) -> {
				fileSystems.remove(s);
			});
			return fileSystem;
		}
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		return currentUserFileSystem();
	}

	@Override
	public Path getPath(URI uri) {
		CmsFileSystem fileSystem = currentUserFileSystem();
		String path = uri.getPath();
		if (fileSystem == null)
			try {
				fileSystem = (CmsFileSystem) newFileSystem(uri, new HashMap<String, Object>());
			} catch (IOException e) {
				throw new UncheckedIOException("Could not autocreate file system for " + uri, e);
			}
		return fileSystem.getPath(path);
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		CmsPath cmsPath = (CmsPath) dir;
		return new ContentDirectoryStream(cmsPath, filter);
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		CmsPath cmsPath = (CmsPath) dir;
		ProvidedSession contentSession = cmsPath.getContentSession();
		if (contentSession.exists(dir.toString()))
			throw new FileAlreadyExistsException(dir.toString());

		CmsPath parent = (CmsPath) cmsPath.getParent();
		if (!contentSession.exists(parent.toString()))
			throw new NoSuchFileException(parent.toString());
		// TODO use a proper naming context
		QName fileName = NamespaceUtils.parsePrefixedName(dir.getFileName().toString());
		parent.getContent().add(fileName, DName.collection);
	}

	@Override
	public void delete(Path path) throws IOException {
		CmsPath cmsPath = (CmsPath) path;
		ProvidedSession contentSession = cmsPath.getContentSession();
		if (!contentSession.exists(cmsPath.toString()))
			throw new NoSuchFileException(cmsPath.toString());
		contentSession.edit((s) -> {
			cmsPath.getContent().remove();
		});
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		// TODO make it smarter
		return path.toString().equals(path2.toString());
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		return false;
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		CmsFileSystem fileSystem = (CmsFileSystem) path.getFileSystem();
		return fileSystem.getFileStore(path.toString());
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * UTILITIES
	 */

	CmsFileSystem currentUserFileSystem() {
		CmsSession cmsSession = CurrentUser.getCmsSession();
		return fileSystems.get(cmsSession);
	}

	void close(CmsFileSystem fileSystem) {
		CmsSession cmsSession = fileSystem.getCmsSession();
		CmsFileSystem ref = fileSystems.remove(cmsSession);
		assert ref == fileSystem;
	}

	/*
	 * DEPENDENCY INJECTION
	 */
	public void setContentRepository(ProvidedRepository contentRepository) {
		this.contentRepository = contentRepository;
	}

}
