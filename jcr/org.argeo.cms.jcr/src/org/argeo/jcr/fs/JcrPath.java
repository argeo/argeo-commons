package org.argeo.jcr.fs;

import java.nio.file.Path;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.api.acr.fs.AbstractFsPath;

/** A {@link Path} which contains a reference to a JCR {@link Node}. */
public class JcrPath extends AbstractFsPath<JcrFileSystem, WorkspaceFileStore> {
	final static String separator = "/";
	final static char separatorChar = '/';

//	private final JcrFileSystem fs;
//	/** null for non absolute paths */
//	private final WorkspaceFileStore fileStore;
//	private final String[] path;// null means root
//	private final boolean absolute;
//
//	// optim
//	private final int hashCode;

	public JcrPath(JcrFileSystem filesSystem, String path) {
		super(filesSystem, path);
//		this.fs = filesSystem;
//		if (path == null)
//			throw new JcrFsException("Path cannot be null");
//		if (path.equals(separator)) {// root
//			this.path = null;
//			this.absolute = true;
//			this.hashCode = 0;
//			this.fileStore = fs.getBaseFileStore();
//			return;
//		} else if (path.equals("")) {// empty path
//			this.path = new String[] { "" };
//			this.absolute = false;
//			this.fileStore = null;
//			this.hashCode = "".hashCode();
//			return;
//		}
//
//		if (path.equals("~")) {// home
//			path = filesSystem.getUserHomePath();
//			if (path == null)
//				throw new JcrFsException("No home directory available");
//		}
//
//		this.absolute = path.charAt(0) == separatorChar ? true : false;
//
//		this.fileStore = absolute ? fs.getFileStore(path) : null;
//
//		String trimmedPath = path.substring(absolute ? 1 : 0,
//				path.charAt(path.length() - 1) == separatorChar ? path.length() - 1 : path.length());
//		this.path = trimmedPath.split(separator);
//		for (int i = 0; i < this.path.length; i++) {
//			this.path[i] = Text.unescapeIllegalJcrChars(this.path[i]);
//		}
//		this.hashCode = this.path[this.path.length - 1].hashCode();
//		assert !(absolute && fileStore == null);
	}

	public JcrPath(JcrFileSystem filesSystem, Node node) throws RepositoryException {
		this(filesSystem, filesSystem.getFileStore(node).toFsPath(node));
	}

	/** Internal optimisation */
	private JcrPath(JcrFileSystem filesSystem, WorkspaceFileStore fileStore, String[] path, boolean absolute) {
		super(filesSystem, fileStore, path, absolute);
//		this.fs = filesSystem;
//		this.path = path;
//		this.absolute = path == null ? true : absolute;
//		if (this.absolute && fileStore == null)
//			throw new IllegalArgumentException("Absolute path requires a file store");
//		if (!this.absolute && fileStore != null)
//			throw new IllegalArgumentException("A file store should not be provided for a relative path");
//		this.fileStore = fileStore;
//		this.hashCode = path == null ? 0 : path[path.length - 1].hashCode();
//		assert !(absolute && fileStore == null);
	}

	protected String cleanUpSegment(String segment) {
		return Text.unescapeIllegalJcrChars(segment);
	}

	@Override
	protected JcrPath newInstance(String path) {
		return new JcrPath(getFileSystem(), path);
	}

	@Override
	protected JcrPath newInstance(String[] segments, boolean absolute) {
		return new JcrPath(getFileSystem(), getFileStore(), segments, absolute);

	}

//	@Override
//	public FileSystem getFileSystem() {
//		return fs;
//	}
//
//	@Override
//	public boolean isAbsolute() {
//		return absolute;
//	}
//
//	@Override
//	public Path getRoot() {
//		if (path == null)
//			return this;
//		return new JcrPath(fs, separator);
//	}
//
//	@Override
//	public String toString() {
//		return toFsPath(path);
//	}
//
//	private String toFsPath(String[] path) {
//		if (path == null)
//			return "/";
//		StringBuilder sb = new StringBuilder();
//		if (isAbsolute())
//			sb.append('/');
//		for (int i = 0; i < path.length; i++) {
//			if (i != 0)
//				sb.append('/');
//			sb.append(path[i]);
//		}
//		return sb.toString();
//	}

//	@Deprecated
//	private String toJcrPath() {
//		return toJcrPath(path);
//	}
//
//	@Deprecated
//	private String toJcrPath(String[] path) {
//		if (path == null)
//			return "/";
//		StringBuilder sb = new StringBuilder();
//		if (isAbsolute())
//			sb.append('/');
//		for (int i = 0; i < path.length; i++) {
//			if (i != 0)
//				sb.append('/');
//			sb.append(Text.escapeIllegalJcrChars(path[i]));
//		}
//		return sb.toString();
//	}

//	@Override
//	public Path getFileName() {
//		if (path == null)
//			return null;
//		return new JcrPath(fs, path[path.length - 1]);
//	}
//
//	@Override
//	public Path getParent() {
//		if (path == null)
//			return null;
//		if (path.length == 1)// root
//			return new JcrPath(fs, separator);
//		String[] parentPath = Arrays.copyOfRange(path, 0, path.length - 1);
//		if (!absolute)
//			return new JcrPath(fs, null, parentPath, absolute);
//		else
//			return new JcrPath(fs, toFsPath(parentPath));
//	}
//
//	@Override
//	public int getNameCount() {
//		if (path == null)
//			return 0;
//		return path.length;
//	}
//
//	@Override
//	public Path getName(int index) {
//		if (path == null)
//			return null;
//		return new JcrPath(fs, path[index]);
//	}
//
//	@Override
//	public Path subpath(int beginIndex, int endIndex) {
//		if (path == null)
//			return null;
//		String[] parentPath = Arrays.copyOfRange(path, beginIndex, endIndex);
//		return new JcrPath(fs, null, parentPath, false);
//	}
//
//	@Override
//	public boolean startsWith(Path other) {
//		return toString().startsWith(other.toString());
//	}
//
//	@Override
//	public boolean startsWith(String other) {
//		return toString().startsWith(other);
//	}
//
//	@Override
//	public boolean endsWith(Path other) {
//		return toString().endsWith(other.toString());
//	}
//
//	@Override
//	public boolean endsWith(String other) {
//		return toString().endsWith(other);
//	}

//	@Override
//	public Path normalize() {
//		// always normalized
//		return this;
//	}

//	@Override
//	public Path resolve(Path other) {
//		JcrPath otherPath = (JcrPath) other;
//		if (otherPath.isAbsolute())
//			return other;
//		String[] newPath;
//		if (path == null) {
//			newPath = new String[otherPath.path.length];
//			System.arraycopy(otherPath.path, 0, newPath, 0, otherPath.path.length);
//		} else {
//			newPath = new String[path.length + otherPath.path.length];
//			System.arraycopy(path, 0, newPath, 0, path.length);
//			System.arraycopy(otherPath.path, 0, newPath, path.length, otherPath.path.length);
//		}
//		if (!absolute)
//			return new JcrPath(fs, null, newPath, absolute);
//		else {
//			return new JcrPath(fs, toFsPath(newPath));
//		}
//	}
//
//	@Override
//	public final Path resolve(String other) {
//		return resolve(getFileSystem().getPath(other));
//	}
//
//	@Override
//	public final Path resolveSibling(Path other) {
//		if (other == null)
//			throw new NullPointerException();
//		Path parent = getParent();
//		return (parent == null) ? other : parent.resolve(other);
//	}
//
//	@Override
//	public final Path resolveSibling(String other) {
//		return resolveSibling(getFileSystem().getPath(other));
//	}
//
//	@Override
//	public final Iterator<Path> iterator() {
//		return new Iterator<Path>() {
//			private int i = 0;
//
//			@Override
//			public boolean hasNext() {
//				return (i < getNameCount());
//			}
//
//			@Override
//			public Path next() {
//				if (i < getNameCount()) {
//					Path result = getName(i);
//					i++;
//					return result;
//				} else {
//					throw new NoSuchElementException();
//				}
//			}
//
//			@Override
//			public void remove() {
//				throw new UnsupportedOperationException();
//			}
//		};
//	}
//
//	@Override
//	public Path relativize(Path other) {
//		if (equals(other))
//			return new JcrPath(fs, "");
//		if (other.startsWith(this)) {
//			String p1 = toString();
//			String p2 = other.toString();
//			String relative = p2.substring(p1.length(), p2.length());
//			if (relative.charAt(0) == '/')
//				relative = relative.substring(1);
//			return new JcrPath(fs, relative);
//		}
//		throw new IllegalArgumentException(other + " cannot be relativized against " + this);
//	}

//	@Override
//	public URI toUri() {
//		try {
//			return new URI(fs.provider().getScheme(), toString(), null);
//		} catch (URISyntaxException e) {
//			throw new JcrFsException("Cannot create URI for " + toString(), e);
//		}
//	}
//
//	@Override
//	public Path toAbsolutePath() {
//		if (isAbsolute())
//			return this;
//		return new JcrPath(fs, fileStore, path, true);
//	}
//
//	@Override
//	public Path toRealPath(LinkOption... options) throws IOException {
//		return this;
//	}
//
//	@Override
//	public File toFile() {
//		throw new UnsupportedOperationException();
//	}

	public Node getNode() throws RepositoryException {
		if (!isAbsolute())// TODO default dir
			throw new JcrFsException("Cannot get a JCR node from a relative path");
		assert getFileStore() != null;
		return getFileStore().toNode(getSegments());
//		String pathStr = toJcrPath();
//		Session session = fs.getSession();
//		// TODO synchronize on the session ?
//		if (!session.itemExists(pathStr))
//			return null;
//		return session.getNode(pathStr);
	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (!(obj instanceof JcrPath))
//			return false;
//		JcrPath other = (JcrPath) obj;
//
//		if (path == null) {// root
//			if (other.path == null)// root
//				return true;
//			else
//				return false;
//		} else {
//			if (other.path == null)// root
//				return false;
//		}
//		// non root
//		if (path.length != other.path.length)
//			return false;
//		for (int i = 0; i < path.length; i++) {
//			if (!path[i].equals(other.path[i]))
//				return false;
//		}
//		return true;
//	}

//	@Override
//	public int hashCode() {
//		return hashCode;
//	}

//	@Override
//	protected Object clone() throws CloneNotSupportedException {
//		return new JcrPath(fs, toString());
//	}

//	@Override
//	protected void finalize() throws Throwable {
//		Arrays.fill(path, null);
//	}

	
	
}
