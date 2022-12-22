package org.argeo.cms.acr.fs;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.ContentResourceException;
import org.argeo.api.acr.CrAttributeType;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.DName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.acr.AbstractContent;
import org.argeo.cms.acr.ContentUtils;
import org.argeo.cms.util.FsUtils;

/** Content persisted as a filesystem {@link Path}. */
public class FsContent extends AbstractContent implements ProvidedContent {
	private CmsLog log = CmsLog.getLog(FsContent.class);

	final static String USER_ = "user:";

	private static final Map<QName, String> BASIC_KEYS;
	private static final Map<QName, String> POSIX_KEYS;
	static {
		BASIC_KEYS = new HashMap<>();
		BASIC_KEYS.put(DName.creationdate.qName(), "basic:creationTime");
		BASIC_KEYS.put(DName.getlastmodified.qName(), "basic:lastModifiedTime");
		BASIC_KEYS.put(DName.getcontentlength.qName(), "basic:size");

		BASIC_KEYS.put(CrName.fileKey.qName(), "basic:fileKey");

		POSIX_KEYS = new HashMap<>(BASIC_KEYS);
		POSIX_KEYS.put(DName.owner.qName(), "owner:owner");
		POSIX_KEYS.put(DName.group.qName(), "posix:group");
		POSIX_KEYS.put(CrName.permissions.qName(), "posix:permissions");
	}

	private final FsContentProvider provider;
	private final Path path;
	private final boolean isMountBase;
	private final QName name;

	protected FsContent(ProvidedSession session, FsContentProvider contentProvider, Path path) {
		super(session);
		this.provider = contentProvider;
		this.path = path;
		this.isMountBase = contentProvider.isMountBase(path);
		// TODO check file names with ':' ?
		if (isMountBase) {
			String mountPath = provider.getMountPath();
			if (mountPath != null && !mountPath.equals(ContentUtils.ROOT_SLASH)) {
				Content mountPoint = session.getMountPoint(mountPath);
				this.name = mountPoint.getName();
			} else {
				this.name = CrName.root.qName();
			}
		} else {

			// TODO should we support prefixed name for known types?
			QName providerName = provider.fromFsPrefixedName(path.getFileName().toString());
//			QName providerName = new QName(path.getFileName().toString());
			// TODO remove extension if mounted?
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

	@SuppressWarnings("unchecked")
	@Override
	public <A> Optional<A> get(QName key, Class<A> clss) {
		Object value;
		try {
			// We need to add user: when accessing via Files#getAttribute

			if (POSIX_KEYS.containsKey(key)) {
				value = Files.getAttribute(path, toFsAttributeKey(key));
			} else {
				UserDefinedFileAttributeView udfav = Files.getFileAttributeView(path,
						UserDefinedFileAttributeView.class);
				String prefixedName = provider.toFsPrefixedName(key);
				if (!udfav.list().contains(prefixedName))
					return Optional.empty();
				ByteBuffer buf = ByteBuffer.allocate(udfav.size(prefixedName));
				udfav.read(prefixedName, buf);
				buf.flip();
				if (buf.hasArray())
					value = buf.array();
				else {
					byte[] arr = new byte[buf.remaining()];
					buf.get(arr);
					value = arr;
				}
			}
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

		// TODO better deal with multiple types
		if (value instanceof byte[]) {
			String str = new String((byte[]) value, StandardCharsets.UTF_8);
			String[] arr = str.split("\n");

			if (arr.length == 1) {
				if (clss.isAssignableFrom(String.class)) {
					res = (A) arr[0];
				} else {
					res = (A) CrAttributeType.parse(arr[0]);
				}
			} else {
				List<Object> lst = new ArrayList<>();
				for (String s : arr) {
					lst.add(CrAttributeType.parse(s));
				}
				res = (A) lst;
			}
		}
		if (res == null) {
			if (isDefaultAttrTypeRequested(clss))
				return Optional.of((A) CrAttributeType.parse(value.toString()));
			if (clss.isAssignableFrom(value.getClass()))
				return Optional.of((A) value);
			if (clss.isAssignableFrom(String.class))
				return Optional.of((A) value.toString());
			log.warn("Cannot interpret " + key + " in " + this);
			return Optional.empty();
//			try {
//				res = (A) value;
//			} catch (ClassCastException e) {
//				return Optional.empty();
//			}
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
					QName providerName = provider.fromFsPrefixedName(name);
					if (providerName.getNamespaceURI().equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI))
						continue; // skip prefix mapping
					QName sessionName = new ContentName(providerName, getSession());
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
			udfav.delete(provider.toFsPrefixedName(key));
		} catch (IOException e) {
			throw new ContentResourceException("Cannot delete attribute " + key + " for " + path, e);
		}
	}

	@Override
	public Object put(QName key, Object value) {
		Object previous = get(key);

		String toWrite;
		if (value instanceof List) {
			StringJoiner sj = new StringJoiner("\n");
			for (Object obj : (List<?>) value) {
				sj.add(obj.toString());
			}
			toWrite = sj.toString();
		} else {
			toWrite = value.toString();
		}

		UserDefinedFileAttributeView udfav = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
		ByteBuffer bb = ByteBuffer.wrap(toWrite.getBytes(StandardCharsets.UTF_8));
		try {
			udfav.write(provider.toFsPrefixedName(key), bb);
		} catch (IOException e) {
			throw new ContentResourceException("Cannot delete attribute " + key + " for " + path, e);
		}
		return previous;
	}

	protected String toFsAttributeKey(QName key) {
		if (POSIX_KEYS.containsKey(key))
			return POSIX_KEYS.get(key);
		else
			return USER_ + provider.toFsPrefixedName(key);
	}

	/*
	 * CONTENT OPERATIONS
	 */
	@Override
	public Iterator<Content> iterator() {
		if (Files.isDirectory(path)) {
			try {
				return Files.list(path).map((p) -> {
					FsContent fsContent = new FsContent(this, p);
					Optional<String> isMount = fsContent.get(CrName.mount.qName(), String.class);
					if (isMount.orElse("false").equals("true")) {
						QName[] classes = null;
						ContentProvider contentProvider = getSession().getRepository()
								.getMountContentProvider(fsContent, false, classes);
						Content mountedContent = contentProvider.get(getSession(), "");
						return mountedContent;
					} else {
						return (Content) fsContent;
					}
				}).iterator();
			} catch (IOException e) {
				throw new ContentResourceException("Cannot list " + path, e);
			}
		} else {
			return Collections.emptyIterator();
		}
	}

	@Override
	public Content add(QName name, QName... classes) {
		FsContent fsContent;
		try {
			Path newPath = path.resolve(provider.toFsPrefixedName(name));
			if (ContentName.contains(classes, DName.collection.qName()))
				Files.createDirectory(newPath);
			else
				Files.createFile(newPath);

//		for(ContentClass clss:classes) {
//			Files.setAttribute(newPath, name, newPath, null)
//		}
			fsContent = new FsContent(this, newPath);
		} catch (IOException e) {
			throw new ContentResourceException("Cannot create new content", e);
		}

		if (classes.length > 0)
			fsContent.addContentClasses(classes);
		if (getSession().getRepository().shouldMount(classes)) {
			ContentProvider contentProvider = getSession().getRepository().getMountContentProvider(fsContent, true,
					classes);
			Content mountedContent = contentProvider.get(getSession(), "");
			fsContent.put(CrName.mount.qName(), "true");
			return mountedContent;

		} else {
			return fsContent;
		}
	}

	@Override
	public void remove() {
		FsUtils.delete(path);
	}

	@Override
	public Content getParent() {
		if (isMountBase) {
			String mountPath = provider.getMountPath();
			if (mountPath == null || mountPath.equals("/"))
				return null;
			String[] parent = ContentUtils.getParentPath(mountPath);
			return getSession().get(parent[0]);
		}
		return new FsContent(this, path.getParent());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C extends Closeable> C open(Class<C> clss) throws IOException, IllegalArgumentException {
		if (InputStream.class.isAssignableFrom(clss)) {
			if (Files.isDirectory(path))
				throw new UnsupportedOperationException("Cannot open " + path + " as stream, since it is a directory");
			return (C) Files.newInputStream(path);
		} else if (OutputStream.class.isAssignableFrom(clss)) {
			if (Files.isDirectory(path))
				throw new UnsupportedOperationException("Cannot open " + path + " as stream, since it is a directory");
			return (C) Files.newOutputStream(path);
		}
		return super.open(clss);
	}

	/*
	 * MOUNT MANAGEMENT
	 */
	@Override
	public ProvidedContent getMountPoint(String relativePath) {
		Path childPath = path.resolve(relativePath);
		// TODO check that it is a mount
		return new FsContent(this, childPath);
	}

	/*
	 * TYPING
	 */

	@Override
	public List<QName> getContentClasses() {
		List<QName> res = new ArrayList<>();
		List<String> value = getMultiple(DName.resourcetype.qName(), String.class);
		for (String s : value) {
			QName name = NamespaceUtils.parsePrefixedName(provider, s);
			res.add(name);
		}
		if (Files.isDirectory(path))
			res.add(DName.collection.qName());
		return res;
	}

	@Override
	public void addContentClasses(QName... contentClass) {
		List<String> toWrite = new ArrayList<>();
		for (QName cc : getContentClasses()) {
			if (cc.equals(DName.collection.qName()))
				continue; // skip
			toWrite.add(NamespaceUtils.toPrefixedName(provider, cc));
		}
		for (QName cc : contentClass) {
			toWrite.add(NamespaceUtils.toPrefixedName(provider, cc));
		}
		put(DName.resourcetype.qName(), toWrite);
	}

	/*
	 * ACCESSORS
	 */

	@Override
	public FsContentProvider getProvider() {
		return provider;
	}

	/*
	 * READ / WRITE
	 */
	@SuppressWarnings("unchecked")
	public <A> CompletableFuture<A> write(Class<A> clss) {
		if (isContentClass(DName.collection.qName())) {
			throw new IllegalStateException("Cannot directly write to a collection");
		}
		if (InputStream.class.isAssignableFrom(clss)) {
			CompletableFuture<InputStream> res = new CompletableFuture<>();
			res.thenAccept((in) -> {
				try {
					Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new RuntimeException("Cannot write to " + path, e);
				}
			});
			return (CompletableFuture<A>) res;
		} else if (Source.class.isAssignableFrom(clss)) {
			CompletableFuture<Source> res = new CompletableFuture<Source>();
			res.thenAccept((source) -> {
//				Path targetPath = path.getParent().resolve(path.getFileName()+".xml");
				Path targetPath = path;
				try (OutputStream out = Files.newOutputStream(targetPath)) {
					StreamResult result = new StreamResult(out);
					TransformerFactory.newDefaultInstance().newTransformer().transform(source, result);
				} catch (IOException | TransformerException e) {
					throw new RuntimeException("Cannot write to " + path, e);
				}
			});
			return (CompletableFuture<A>) res;
		} else {
			return super.write(clss);
		}
	}
}
