package org.argeo.cms.acr.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.ContentResourceException;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.AbstractContent;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.util.FsUtils;

public class FsContent extends AbstractContent implements ProvidedContent {
	final static String USER_ = "user:";

	private static final Map<QName, String> BASIC_KEYS;
	private static final Map<QName, String> POSIX_KEYS;
	static {
		BASIC_KEYS = new HashMap<>();
		BASIC_KEYS.put(CrName.CREATION_TIME.get(), "basic:creationTime");
		BASIC_KEYS.put(CrName.LAST_MODIFIED_TIME.get(), "basic:lastModifiedTime");
		BASIC_KEYS.put(CrName.SIZE.get(), "basic:size");
		BASIC_KEYS.put(CrName.FILE_KEY.get(), "basic:fileKey");

		POSIX_KEYS = new HashMap<>(BASIC_KEYS);
		POSIX_KEYS.put(CrName.OWNER.get(), "owner:owner");
		POSIX_KEYS.put(CrName.GROUP.get(), "posix:group");
		POSIX_KEYS.put(CrName.PERMISSIONS.get(), "posix:permissions");
	}

	private final ProvidedSession session;
	private final FsContentProvider provider;
	private final Path path;
	private final boolean isRoot;
	private final QName name;

	protected FsContent(ProvidedSession session, FsContentProvider contentProvider, Path path) {
		this.session = session;
		this.provider = contentProvider;
		this.path = path;
		this.isRoot = contentProvider.isRoot(path);
		// TODO check file names with ':' ?
		if (isRoot)
			this.name = CrName.ROOT.get();
		else {
			QName providerName = NamespaceUtils.parsePrefixedName(provider, path.getFileName().toString());
			this.name = new ContentName(providerName, session);
		}
	}

	protected FsContent(FsContent context, Path path) {
		this(context.getSession(), context.getProvider(), path);
	}

	private boolean isPosix() {
		return path.getFileSystem().supportedFileAttributeViews().contains("posix");
	}

	@Override
	public QName getName() {
		return name;
	}

	/*
	 * ATTRIBUTES
	 */

	@Override
	public <A> Optional<A> get(QName key, Class<A> clss) {
		Object value;
		try {
			// We need to add user: when accessing via Files#getAttribute
			value = Files.getAttribute(path, toFsAttributeKey(key));
		} catch (IOException e) {
			throw new ContentResourceException("Cannot retrieve attribute " + key + " for " + path, e);
		}
		A res = null;
		if (value instanceof FileTime) {
			if (clss.isAssignableFrom(FileTime.class))
				res = (A) value;
			Instant instant = ((FileTime) value).toInstant();
			if (Object.class.isAssignableFrom(clss)) {// plain object requested
				res = (A) instant;
			}
			// TODO perform trivial file conversion to other formats
		}
		if (value instanceof byte[]) {
			res = (A) new String((byte[]) value, StandardCharsets.UTF_8);
		}
		if (res == null)
			try {
				res = (A) value;
			} catch (ClassCastException e) {
				return Optional.empty();
			}
		return Optional.of(res);
	}

	@Override
	protected Iterable<QName> keys() {
		Set<QName> result = new HashSet<>(isPosix() ? POSIX_KEYS.keySet() : BASIC_KEYS.keySet());
		UserDefinedFileAttributeView udfav = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
		if (udfav != null) {
			try {
				for (String name : udfav.list()) {
					QName providerName = NamespaceUtils.parsePrefixedName(provider, name);
					QName sessionName = new ContentName(providerName, session);
					result.add(sessionName);
				}
			} catch (IOException e) {
				throw new ContentResourceException("Cannot list attributes for " + path, e);
			}
		}
		return result;
	}

	@Override
	protected void removeAttr(QName key) {
		UserDefinedFileAttributeView udfav = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
		try {
			udfav.delete(NamespaceUtils.toPrefixedName(provider, key));
		} catch (IOException e) {
			throw new ContentResourceException("Cannot delete attribute " + key + " for " + path, e);
		}
	}

	@Override
	public Object put(QName key, Object value) {
		Object previous = get(key);
		UserDefinedFileAttributeView udfav = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
		ByteBuffer bb = ByteBuffer.wrap(value.toString().getBytes(StandardCharsets.UTF_8));
		try {
			int size = udfav.write(NamespaceUtils.toPrefixedName(provider, key), bb);
		} catch (IOException e) {
			throw new ContentResourceException("Cannot delete attribute " + key + " for " + path, e);
		}
		return previous;
	}

	protected String toFsAttributeKey(QName key) {
		if (POSIX_KEYS.containsKey(key))
			return POSIX_KEYS.get(key);
		else
			return USER_ + NamespaceUtils.toPrefixedName(provider, key);
	}

	/*
	 * CONTENT OPERATIONS
	 */
	@Override
	public Iterator<Content> iterator() {
		if (Files.isDirectory(path)) {
			try {
				return Files.list(path).map((p) -> (Content) new FsContent(this, p)).iterator();
			} catch (IOException e) {
				throw new ContentResourceException("Cannot list " + path, e);
			}
		} else {
			return Collections.emptyIterator();
		}
	}

	@Override
	public Content add(QName name, QName... classes) {
		try {
			Path newPath = path.resolve(NamespaceUtils.toPrefixedName(provider, name));
			if (ContentName.contains(classes, CrName.COLLECTION.get()))
				Files.createDirectory(newPath);
			else
				Files.createFile(newPath);

//		for(ContentClass clss:classes) {
//			Files.setAttribute(newPath, name, newPath, null)
//		}
			return new FsContent(this, newPath);
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
		return new FsContent(this, path.getParent());
	}

	/*
	 * ACCESSORS
	 */
	@Override
	public ProvidedSession getSession() {
		return session;
	}

	@Override
	public FsContentProvider getProvider() {
		return provider;
	}

}
