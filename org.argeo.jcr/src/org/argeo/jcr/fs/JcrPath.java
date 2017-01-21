package org.argeo.jcr.fs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class JcrPath implements Path {
	private final static String delimStr = "/";
	private final static char delimChar = '/';

	private final JcrFileSystem fs;
	private final String[] path;// null means root
	private final boolean absolute;

	// optim
	private final int hashCode;

	public JcrPath(JcrFileSystem filesSystem, String path) {
		// this(filesSystem, path.equals("/") ? null : path.split("/"), path ==
		// null ? true : path.startsWith("/"));
		this.fs = filesSystem;
		if (path == null)
			throw new JcrFsException("Path cannot be null");
		if (path.equals(delimStr)) {// root
			this.path = null;
			this.absolute = true;
			this.hashCode = 0;
			return;
		} else if (path.equals("")) {// empty path
			this.path = new String[] { "" };
			this.absolute = false;
			this.hashCode = "".hashCode();
			return;
		}
		this.absolute = path.charAt(0) == delimChar ? true : false;
		String trimmedPath = path.substring(absolute ? 1 : 0,
				path.charAt(path.length() - 1) == delimChar ? path.length() - 1 : path.length());
		this.path = trimmedPath.split(delimStr);
		this.hashCode = this.path[this.path.length - 1].hashCode();
	}

	public JcrPath(JcrFileSystem filesSystem, Node node) throws RepositoryException {
		this(filesSystem, node.getPath());
	}

	/** Internal optimisation */
	private JcrPath(JcrFileSystem filesSystem, String[] path, boolean absolute) {
		this.fs = filesSystem;
		this.path = path;
		this.absolute = path == null ? true : absolute;
		this.hashCode = path == null ? 0 : path[path.length - 1].hashCode();
	}

	@Override
	public FileSystem getFileSystem() {
		return fs;
	}

	@Override
	public boolean isAbsolute() {
		return absolute;
	}

	@Override
	public Path getRoot() {
		try {
			if (path == null)
				return this;
			return new JcrPath(fs, fs.getSession().getRootNode());
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot get root", e);
		}
	}

	@Override
	public String toString() {
		if (path == null)
			return "/";
		StringBuilder sb = new StringBuilder();
		if (isAbsolute())
			sb.append('/');
		for (int i = 0; i < path.length; i++) {
			if (i != 0)
				sb.append('/');
			sb.append(path[i]);
		}
		return sb.toString();
	}

	@Override
	public Path getFileName() {
		if (path == null)
			return null;
		return new JcrPath(fs, path[path.length - 1]);
	}

	@Override
	public Path getParent() {
		if (path == null)
			return null;
		if (path.length == 1)// root
			return new JcrPath(fs, delimStr);
		String[] parentPath = Arrays.copyOfRange(path, 0, path.length - 1);
		return new JcrPath(fs, parentPath, absolute);
	}

	@Override
	public int getNameCount() {
		if (path == null)
			return 0;
		return path.length;
	}

	@Override
	public Path getName(int index) {
		if (path == null)
			return null;
		return new JcrPath(fs, path[index]);
	}

	@Override
	public Path subpath(int beginIndex, int endIndex) {
		if (path == null)
			return null;
		String[] parentPath = Arrays.copyOfRange(path, beginIndex, endIndex);
		return new JcrPath(fs, parentPath, false);
	}

	@Override
	public boolean startsWith(Path other) {
		return toString().startsWith(other.toString());
	}

	@Override
	public boolean startsWith(String other) {
		return toString().startsWith(other);
	}

	@Override
	public boolean endsWith(Path other) {
		return toString().endsWith(other.toString());
	}

	@Override
	public boolean endsWith(String other) {
		return toString().endsWith(other);
	}

	@Override
	public Path normalize() {
		// always normalized
		return this;
	}

	@Override
	public Path resolve(Path other) {
		JcrPath otherPath = (JcrPath) other;
		if (otherPath.isAbsolute())
			return other;
		String[] newPath;
		if (path == null) {
			newPath = new String[otherPath.path.length];
			System.arraycopy(otherPath.path, 0, newPath, 0, otherPath.path.length);
		} else {
			newPath = new String[path.length + otherPath.path.length];
			System.arraycopy(path, 0, newPath, 0, path.length);
			System.arraycopy(otherPath.path, 0, newPath, path.length, otherPath.path.length);
		}
		return new JcrPath(fs, newPath, absolute);
	}

	@Override
	public final Path resolve(String other) {
		return resolve(getFileSystem().getPath(other));
	}

	@Override
	public final Path resolveSibling(Path other) {
		if (other == null)
			throw new NullPointerException();
		Path parent = getParent();
		return (parent == null) ? other : parent.resolve(other);
	}

	@Override
	public final Path resolveSibling(String other) {
		return resolveSibling(getFileSystem().getPath(other));
	}

	@Override
	public final Iterator<Path> iterator() {
		return new Iterator<Path>() {
			private int i = 0;

			@Override
			public boolean hasNext() {
				return (i < getNameCount());
			}

			@Override
			public Path next() {
				if (i < getNameCount()) {
					Path result = getName(i);
					i++;
					return result;
				} else {
					throw new NoSuchElementException();
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public Path relativize(Path other) {
		if (equals(other))
			return new JcrPath(fs, "");
		if (other.startsWith(this)) {
			String p1 = toString();
			String p2 = other.toString();
			return new JcrPath(fs, p2.substring(p1.length(), p2.length()));
		}
		throw new IllegalArgumentException(other + " cannot be realtivized against " + this);
	}

	@Override
	public URI toUri() {
		try {
			return new URI("jcr", toString(), null);
		} catch (URISyntaxException e) {
			throw new JcrFsException("Cannot create URI for " + toString(), e);
		}
	}

	@Override
	public Path toAbsolutePath() {
		if (isAbsolute())
			return this;
		return new JcrPath(fs, path, true);
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		return this;
	}

	@Override
	public File toFile() {
		throw new UnsupportedOperationException();
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int compareTo(Path other) {
		return toString().compareTo(other.toString());
	}

	public Node getNode() throws RepositoryException {
		if (!isAbsolute())// TODO default dir
			throw new JcrFsException("Cannot get node from relative path");
		String pathStr = toString();
		Session session = fs.getSession();
		// TODO synchronize on the session ?
		if (!session.itemExists(pathStr))
			return null;
		return session.getNode(pathStr);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JcrPath))
			return false;
		JcrPath other = (JcrPath) obj;

		if (path == null) {// root
			if (other.path == null)// root
				return true;
			else
				return false;
		} else {
			if (other.path == null)// root
				return false;
		}
		// non root
		if (path.length != other.path.length)
			return false;
		for (int i = 0; i < path.length; i++) {
			if (!path[i].equals(other.path[i]))
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new JcrPath(fs, toString());
	}

	@Override
	protected void finalize() throws Throwable {
		Arrays.fill(path, null);
	}

}
