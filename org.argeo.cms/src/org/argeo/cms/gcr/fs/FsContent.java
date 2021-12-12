package org.argeo.cms.gcr.fs;

import java.io.IOException;
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

import org.argeo.api.gcr.AbstractContent;
import org.argeo.api.gcr.Content;
import org.argeo.api.gcr.ContentResourceException;
import org.argeo.api.gcr.ContentSession;

public class FsContent extends AbstractContent implements Content {
	private static final Set<String> BASIC_KEYS = new HashSet<>(
			Arrays.asList("basic:creationTime", "basic:lastModifiedTime", "basic:size", "basic:fileKey"));
	private static final Set<String> POSIX_KEYS;
	static {
		POSIX_KEYS = new HashSet<>(BASIC_KEYS);
		POSIX_KEYS.add("owner:owner");
		POSIX_KEYS.add("posix:group");
		POSIX_KEYS.add("posix:permissions");
	}

	private FsContentSession contentSession;
	private final Path path;

	public FsContent(FsContentSession contentSession, Path path) {
		super();
		this.contentSession = contentSession;
		this.path = path;
	}

	private boolean isPosix() {
		return path.getFileSystem().supportedFileAttributeViews().contains("posix");
	}

	@Override
	public Iterator<Content> iterator() {
		if (Files.isDirectory(path)) {
			try {
				return Files.list(path).map((p) -> (Content) new FsContent(contentSession, p)).iterator();
			} catch (IOException e) {
				throw new ContentResourceException("Cannot list " + path, e);
			}
		} else {
			return Collections.emptyIterator();
		}
	}

	@Override
	public String getName() {
		return path.getFileName().toString();
	}

	@Override
	public <A> A get(String key, Class<A> clss) {
		Object value;
		try {
			value = Files.getAttribute(path, key);
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
	public ContentSession getSession() {
		return contentSession;
	}

	@Override
	protected Iterable<String> keys() {
		Set<String> result = new HashSet<>(isPosix() ? POSIX_KEYS : BASIC_KEYS);
		UserDefinedFileAttributeView udfav = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
		if (udfav != null) {
			try {
				for (String name : udfav.list()) {
					result.add("user:" + name);
				}
			} catch (IOException e) {
				throw new ContentResourceException("Cannot liast attributes for " + path, e);
			}
		}
		return result;
	}

}
