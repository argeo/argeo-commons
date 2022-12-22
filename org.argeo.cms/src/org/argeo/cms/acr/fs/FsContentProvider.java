package org.argeo.cms.acr.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.argeo.api.acr.ArgeoNamespace;
import org.argeo.api.acr.ContentResourceException;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.RuntimeNamespaceContext;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.util.OS;

/** Access a file system as a {@link ContentProvider}. */
public class FsContentProvider implements ContentProvider {

	protected String mountPath;
	protected Path rootPath;

	private NavigableMap<String, String> prefixes = new TreeMap<>();

	private final boolean isNtfs;
	private final String XMLNS_;

	public FsContentProvider(String mountPath, Path rootPath) {
		Objects.requireNonNull(mountPath);
		Objects.requireNonNull(rootPath);

		this.mountPath = mountPath;
		this.rootPath = rootPath;

		this.isNtfs = OS.LOCAL.isMSWindows();
		this.XMLNS_ = isNtfs ? "xmlns%3A" : "xmlns:";

		// FIXME make it more robust
		initNamespaces();
	}

	protected FsContentProvider() {
		this.isNtfs = OS.LOCAL.isMSWindows();
		this.XMLNS_ = isNtfs ? "xmlns%3A" : "xmlns:";
	}

	protected void initNamespaces() {
		try {
			UserDefinedFileAttributeView udfav = Files.getFileAttributeView(rootPath,
					UserDefinedFileAttributeView.class);
			if (udfav == null)
				return;
			for (String name : udfav.list()) {
				if (name.startsWith(XMLNS_)) {
					ByteBuffer buf = ByteBuffer.allocate(udfav.size(name));
					udfav.read(name, buf);
					buf.flip();
					String namespace = StandardCharsets.UTF_8.decode(buf).toString();
					String prefix = name.substring(XMLNS_.length());
					prefixes.put(prefix, namespace);
				}
			}

			// defaults
			addDefaultNamespace(udfav, ArgeoNamespace.CR_DEFAULT_PREFIX, ArgeoNamespace.CR_NAMESPACE_URI);
			addDefaultNamespace(udfav, "basic", ArgeoNamespace.CR_NAMESPACE_URI);
			addDefaultNamespace(udfav, "owner", ArgeoNamespace.CR_NAMESPACE_URI);
			addDefaultNamespace(udfav, "posix", ArgeoNamespace.CR_NAMESPACE_URI);
		} catch (IOException e) {
			throw new RuntimeException("Cannot read namespaces from " + rootPath, e);
		}

	}

	protected void addDefaultNamespace(UserDefinedFileAttributeView udfav, String prefix, String namespace)
			throws IOException {
		if (!prefixes.containsKey(prefix)) {
			ByteBuffer bb = ByteBuffer.wrap(namespace.getBytes(StandardCharsets.UTF_8));
			udfav.write(XMLNS_ + prefix, bb);
			prefixes.put(prefix, namespace);
		}
	}

	public void registerPrefix(String prefix, String namespace) {
		if (prefixes.containsKey(prefix))
			prefixes.remove(prefix);
		try {
			UserDefinedFileAttributeView udfav = Files.getFileAttributeView(rootPath,
					UserDefinedFileAttributeView.class);
			addDefaultNamespace(udfav, prefix, namespace);
		} catch (IOException e) {
			throw new RuntimeException("Cannot register namespace " + prefix + " " + namespace + " on " + rootPath, e);
		}

	}

	@Override
	public String getMountPath() {
		return mountPath;
	}

	boolean isMountBase(Path path) {
		try {
			return Files.isSameFile(rootPath, path);
		} catch (IOException e) {
			throw new ContentResourceException(e);
		}
	}

	@Override
	public ProvidedContent get(ProvidedSession session, String relativePath) {
		return new FsContent(session, this, rootPath.resolve(relativePath));
	}

	@Override
	public boolean exists(ProvidedSession session, String relativePath) {
		return Files.exists(rootPath.resolve(relativePath));
	}

	/*
	 * ATTRIBUTE NAMES
	 */
	/**
	 * Make sure that the prefixed name is compatible with the underlying file
	 * system for file names/attributes (NTFS does not accept :)
	 */
	String toFsPrefixedName(QName key) {
		return isNtfs ? NamespaceUtils.toPrefixedName(this, key).replace(":", "%3A")
				: NamespaceUtils.toPrefixedName(this, key);
	}

	/**
	 * PArse a prefixed name which is compatible with the underlying file system for
	 * file names/attributes (NTFS does not accept :)
	 */
	QName fromFsPrefixedName(String name) {
		return isNtfs ? NamespaceUtils.parsePrefixedName(this, name.replace("%3A", ":"))
				: NamespaceUtils.parsePrefixedName(this, name);
	}

	/*
	 * NAMESPACE CONTEXT
	 */

	@Override
	public String getNamespaceURI(String prefix) {
		return NamespaceUtils.getNamespaceURI((p) -> prefixes.get(p), prefix);
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		Iterator<String> res = NamespaceUtils.getPrefixes((ns) -> prefixes.entrySet().stream()
				.filter(e -> e.getValue().equals(ns)).map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet()),
				namespaceURI);
		if (!res.hasNext()) {
			String prefix = RuntimeNamespaceContext.getNamespaceContext().getPrefix(namespaceURI);
			if (prefix != null) {
				registerPrefix(prefix, namespaceURI);
				return getPrefixes(namespaceURI);
			} else {
				throw new IllegalArgumentException("Unknown namespace " + namespaceURI);
			}
		}
		return res;
	}

}
