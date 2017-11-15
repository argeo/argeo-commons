package org.argeo.jcr.fs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.jcr.JcrUtils;

public class JcrFileSystem extends FileSystem {
	private final JcrFileSystemProvider provider;
	private final Session session;
	private String userHomePath = null;

	public JcrFileSystem(JcrFileSystemProvider provider, Session session) {
		super();
		this.provider = provider;
		this.session = session;
		Node userHome = provider.getUserHome(session);
		if (userHome != null)
			try {
				userHomePath = userHome.getPath();
			} catch (RepositoryException e) {
				throw new JcrFsException("Cannot retrieve user home path", e);
			}
	}

	public String getUserHomePath() {
		return userHomePath;
	}

	@Override
	public FileSystemProvider provider() {
		return provider;
	}

	@Override
	public void close() throws IOException {
		JcrUtils.logoutQuietly(session);
	}

	@Override
	public boolean isOpen() {
		return session.isLive();
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public String getSeparator() {
		return "/";
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		try {
			Set<Path> single = new HashSet<>();
			single.add(new JcrPath(this, session.getRootNode()));
			return single;
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot get root path", e);
		}
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		try {
			String[] prefixes = session.getNamespacePrefixes();
			Set<String> res = new HashSet<>();
			for (String prefix : prefixes)
				res.add(prefix);
			res.add("basic");
			return res;
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot get supported file attributes views", e);
		}
	}

	@Override
	public Path getPath(String first, String... more) {
		StringBuilder sb = new StringBuilder(first);
		// TODO Make it more robust
		for (String part : more)
			sb.append('/').append(part);
		return new JcrPath(this, sb.toString());
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		throw new UnsupportedOperationException();
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public WatchService newWatchService() throws IOException {
		throw new UnsupportedOperationException();
	}

	public Session getSession() {
		return session;
	}

}
