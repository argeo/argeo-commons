package org.argeo.api.acr.fs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class AbstractFsPath<FS extends AbstractFsSystem<ST>, ST extends AbstractFsStore> implements Path {
	private final FS fs;
	/** null for non absolute paths */
	private final ST fileStore;

	private final String[] segments;// null means root
	private final boolean absolute;

	private final String separator;

	// optim
	private final int hashCode;

	public AbstractFsPath(FS filesSystem, String path) {
		if (path == null)
			throw new IllegalArgumentException("Path cannot be null");
		this.fs = filesSystem;
		this.separator = fs.getSeparator();
		// TODO deal with both path and separator being empty strings
		if (path.equals(separator)) {// root
			this.segments = null;
			this.absolute = true;
			this.hashCode = 0;
			this.fileStore = fs.getBaseFileStore();
			return;
		} else if (path.equals("")) {// empty path
			this.segments = new String[] { "" };
			this.absolute = false;
			this.hashCode = "".hashCode();
			this.fileStore = null;
			return;
		}

		this.absolute = path.startsWith(toStringRoot());

		String trimmedPath = path.substring(absolute ? toStringRoot().length() : 0,
				path.endsWith(separator) ? path.length() - separator.length() : path.length());
		this.segments = trimmedPath.split(separator);
		// clean up
		for (int i = 0; i < this.segments.length; i++) {
			this.segments[i] = cleanUpSegment(this.segments[i]);
		}
		this.hashCode = this.segments[this.segments.length - 1].hashCode();

		this.fileStore = isAbsolute() ? fs.getFileStore(path) : null;
	}

	protected AbstractFsPath(FS filesSystem, ST fileStore, String[] segments, boolean absolute) {
		this.segments = segments;
		this.absolute = absolute;
		this.hashCode = segments == null ? 0 : segments[segments.length - 1].hashCode();
		this.separator = filesSystem.getSeparator();
//		super(path, path == null ? true : absolute, filesSystem.getSeparator());
//		assert path == null ? absolute == true : true;
		this.fs = filesSystem;
//		this.path = path;
//		this.absolute = path == null ? true : absolute;
		if (isAbsolute() && fileStore == null)
			throw new IllegalArgumentException("Absolute path requires a file store");
		if (!isAbsolute() && fileStore != null)
			throw new IllegalArgumentException("A file store should not be provided for a relative path");
		this.fileStore = fileStore;
		assert !(absolute && fileStore == null);
	}

	protected Path retrieve(String path) {
		return getFileSystem().getPath(path);
	}

	@Override
	public FS getFileSystem() {
		return fs;
	}

	public ST getFileStore() {
		return fileStore;
	}

	@Override
	public boolean isAbsolute() {
		return absolute;
	}

	@Override
	public URI toUri() {
		try {
			return new URI(fs.provider().getScheme(), toString(), null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Cannot create URI for " + toString(), e);
		}
	}

	@Override
	public Path toAbsolutePath() {
		if (isAbsolute())
			return this;
		// FIXME it doesn't seem right
		return newInstance(getSegments(), true);
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		return this;
	}

	@Override
	public File toFile() {
		throw new UnsupportedOperationException();
	}

	/*
	 * PATH OPERATIONS
	 */
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

	public final Path resolve(String other) {
		return resolve(retrieve(other));
	}

	public boolean startsWith(Path other) {
		return toString().startsWith(other.toString());
	}

	public boolean endsWith(Path other) {
		return toString().endsWith(other.toString());
	}

	@Override
	public Path normalize() {
		// always normalized
		return this;
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
	public int compareTo(Path other) {
		return toString().compareTo(other.toString());
	}

	public Path resolve(Path other) {
		AbstractFsPath<?, ?> otherPath = (AbstractFsPath<?, ?>) other;
		if (otherPath.isAbsolute())
			return other;
		String[] newPath;
		if (isRoot()) {
			newPath = new String[otherPath.segments.length];
			System.arraycopy(otherPath.segments, 0, newPath, 0, otherPath.segments.length);
		} else {
			newPath = new String[segments.length + otherPath.segments.length];
			System.arraycopy(segments, 0, newPath, 0, segments.length);
			System.arraycopy(otherPath.segments, 0, newPath, segments.length, otherPath.segments.length);
		}
		if (!absolute)
			return newInstance(newPath, absolute);
		else {
			return newInstance(toString(newPath));
		}
	}

	public Path relativize(Path other) {
		if (equals(other))
			return newInstance("");
		if (other.toString().startsWith(this.toString())) {
			String p1 = toString();
			String p2 = other.toString();
			String relative = p2.substring(p1.length(), p2.length());
			if (relative.charAt(0) == '/')
				relative = relative.substring(1);
			return newInstance(relative);
		}
		throw new IllegalArgumentException(other + " cannot be relativized against " + this);
	}

	/*
	 * FACTORIES
	 */
	protected abstract AbstractFsPath<FS, ST> newInstance(String path);

	protected abstract AbstractFsPath<FS, ST> newInstance(String[] segments, boolean absolute);

	/*
	 * CUSTOMISATIONS
	 */
	protected String toStringRoot() {
		return separator;
	}

	protected String cleanUpSegment(String segment) {
		return segment;
	}

	protected boolean isRoot() {
		return segments == null;
	}

	protected boolean isEmpty() {
		return segments.length == 1 && "".equals(segments[0]);
	}

	/*
	 * PATH OPERATIONS
	 */
	public AbstractFsPath<FS, ST> getRoot() {
		return newInstance(toStringRoot());
	}

	public AbstractFsPath<FS, ST> getParent() {
		if (isRoot())
			return null;
		// FIXME empty path?
		if (segments.length == 1)// first level
			return newInstance(toStringRoot());
		String[] parentPath = Arrays.copyOfRange(segments, 0, segments.length - 1);
		if (!absolute)
			return newInstance(parentPath, absolute);
		else
			return newInstance(toString(parentPath));
	}

	public AbstractFsPath<FS, ST> getFileName() {
		if (isRoot())
			return null;
		return newInstance(segments[segments.length - 1]);
	}

	public int getNameCount() {
		if (isRoot())
			return 0;
		return segments.length;
	}

	public AbstractFsPath<FS, ST> getName(int index) {
		if (isRoot())
			return null;
		return newInstance(segments[index]);
	}

	public AbstractFsPath<FS, ST> subpath(int beginIndex, int endIndex) {
		if (isRoot())
			return null;
		String[] parentPath = Arrays.copyOfRange(segments, beginIndex, endIndex);
		return newInstance(parentPath, false);
	}

	public boolean startsWith(String other) {
		return toString().startsWith(other);
	}

	public boolean endsWith(String other) {
		return toString().endsWith(other);
	}

	/*
	 * UTILITIES
	 */
	protected String toString(String[] path) {
		if (isRoot())
			return toStringRoot();
		StringBuilder sb = new StringBuilder();
		if (isAbsolute())
			sb.append(separator);
		for (int i = 0; i < path.length; i++) {
			if (i != 0)
				sb.append(separator);
			sb.append(path[i]);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return toString(segments);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof AbstractFsPath))
			return false;
		AbstractFsPath<?, ?> other = (AbstractFsPath<?, ?>) obj;

		if (isRoot()) {// root
			if (other.isRoot())// root
				return true;
			else
				return false;
		} else {
			if (other.isRoot())// root
				return false;
		}
		// non root
		if (segments.length != other.segments.length)
			return false;
		for (int i = 0; i < segments.length; i++) {
			if (!segments[i].equals(other.segments[i]))
				return false;
		}
		return true;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return newInstance(toString());
	}

	/*
	 * GETTERS / SETTERS
	 */
	protected String[] getSegments() {
		return segments;
	}

	protected String getSeparator() {
		return separator;
	}

	/*
	 * UNSUPPORTED
	 */
	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
		throw new UnsupportedOperationException();
	}

}
