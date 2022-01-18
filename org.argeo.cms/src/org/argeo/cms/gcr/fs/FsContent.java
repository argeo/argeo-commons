package org.argeo.cms.gcr.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.argeo.api.gcr.Content;
import org.argeo.api.gcr.ContentResourceException;
import org.argeo.api.gcr.CrName;
import org.argeo.api.gcr.ContentName;
import org.argeo.api.gcr.spi.AbstractContent;
import org.argeo.util.FsUtils;

public class FsContent extends AbstractContent implements Content {
	private final static String USER_ = "user:";

	private static final Set<String> BASIC_KEYS = new HashSet<>(
			Arrays.asList("basic:creationTime", "basic:lastModifiedTime", "basic:size", "basic:fileKey"));
	private static final Set<String> POSIX_KEYS;
	static {
		POSIX_KEYS = new HashSet<>(BASIC_KEYS);
		POSIX_KEYS.add("owner:owner");
		POSIX_KEYS.add("posix:group");
		POSIX_KEYS.add("posix:permissions");
	}

	private final FsContentProvider contentProvider;
	private final Path path;
	private final boolean isRoot;

	public FsContent(FsContentProvider contentProvider, Path path) {
		super();
		this.contentProvider = contentProvider;
		this.path = path;
		this.isRoot = contentProvider.isRoot(path);
	}

	private boolean isPosix() {
		return path.getFileSystem().supportedFileAttributeViews().contains("posix");
	}

	@Override
	public String getName() {
		if (isRoot)
			return "";
		return path.getFileName().toString();
	}

	/*
	 * ATTRIBUTES
	 */

	@Override
	public <A> A get(String key, Class<A> clss) {
		Object value;
		try {
			// We need to add user: when accessing via Files#getAttribute
			value = Files.getAttribute(path, toFsAttributeKey(key));
		} catch (IOException e) {
			throw new ContentResourceException("Cannot retrieve attribute " + key + " for " + path, e);
		}
		if (value instanceof FileTime) {
			if (clss.isAssignableFrom(FileTime.class))
				return (A) value;
			Instant instant = ((FileTime) value).toInstant();
			if (Object.class.isAssignableFrom(clss)) {// plain object requested
				return (A) instant;
			}
			// TODO perform trivial file conversion to other formats
		}
		if (value instanceof byte[]) {
			return (A) new String((byte[]) value, StandardCharsets.UTF_8);
		}
		return (A) value;
	}

	@Override
	protected Iterable<String> keys() {
		Set<String> result = new HashSet<>(isPosix() ? POSIX_KEYS : BASIC_KEYS);
		UserDefinedFileAttributeView udfav = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
		if (udfav != null) {
			try {
				for (String name : udfav.list()) {
					result.add(name);
				}
			} catch (IOException e) {
				throw new ContentResourceException("Cannot list attributes for " + path, e);
			}
		}
		return result;
	}

	@Override
	protected void removeAttr(String key) {
		UserDefinedFileAttributeView udfav = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
		try {
			udfav.delete(key);
		} catch (IOException e) {
			throw new ContentResourceException("Cannot delete attribute " + key + " for " + path, e);
		}
	}

	@Override
	public Object put(String key, Object value) {
		Object previous = get(key);
		UserDefinedFileAttributeView udfav = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
		ByteBuffer bb = ByteBuffer.wrap(value.toString().getBytes(StandardCharsets.UTF_8));
		try {
			int size = udfav.write(key, bb);
		} catch (IOException e) {
			throw new ContentResourceException("Cannot delete attribute " + key + " for " + path, e);
		}
		return previous;
	}

	protected String toFsAttributeKey(String key) {
		if (POSIX_KEYS.contains(key))
			return key;
		else
			return USER_ + key;
	}

	/*
	 * CONTENT OPERATIONS
	 */
	@Override
	public Iterator<Content> iterator() {
		if (Files.isDirectory(path)) {
			try {
				return Files.list(path).map((p) -> (Content) new FsContent(contentProvider, p)).iterator();
			} catch (IOException e) {
				throw new ContentResourceException("Cannot list " + path, e);
			}
		} else {
			return Collections.emptyIterator();
		}
	}

	@Override
	public Content add(String name, ContentName... classes) {
		try {
			Path newPath = path.resolve(name);
			if (ContentName.contains(classes, CrName.COLLECTION))
				Files.createDirectory(newPath);
			else
				Files.createFile(newPath);

//		for(ContentClass clss:classes) {
//			Files.setAttribute(newPath, name, newPath, null)
//		}
			return new FsContent(contentProvider, newPath);
		} catch (IOException e) {
			throw new ContentResourceException("Cannot create new content", e);
		}
	}

	@Override
	public void remove() {
		FsUtils.delete(path);
	}

	@Override
	public Content getParent() {
		if (isRoot)
			return null;// TODO deal with mounts
		return new FsContent(contentProvider, path.getParent());
	}

}
