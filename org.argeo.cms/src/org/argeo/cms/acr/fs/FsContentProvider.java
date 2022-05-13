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
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentResourceException;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedSession;

/** Access a file system as a {@link ContentProvider}. */
public class FsContentProvider implements ContentProvider {
	final static String XMLNS_ = "xmlns:";

	private final Path rootPath;
	private final boolean isRoot;

	private NavigableMap<String, String> prefixes = new TreeMap<>();

	public FsContentProvider(Path rootPath, boolean isRoot) {
		this.rootPath = rootPath;
		this.isRoot = isRoot;
		initNamespaces();
	}

	protected void initNamespaces() {
		try {
			UserDefinedFileAttributeView udfav = Files.getFileAttributeView(rootPath,
					UserDefinedFileAttributeView.class);
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
			addDefaultNamespace(udfav, CrName.CR_DEFAULT_PREFIX, CrName.CR_NAMESPACE_URI);
			addDefaultNamespace(udfav, "basic", CrName.CR_NAMESPACE_URI);
			addDefaultNamespace(udfav, "owner", CrName.CR_NAMESPACE_URI);
			addDefaultNamespace(udfav, "posix", CrName.CR_NAMESPACE_URI);
		} catch (IOException e) {
			throw new RuntimeException("Cannot read namespaces from " + rootPath, e);
		}

	}

	protected void addDefaultNamespace(UserDefinedFileAttributeView udfav, String prefix, String namespace)
			throws IOException {
		if (!prefixes.containsKey(prefix)) {
			ByteBuffer bb = ByteBuffer.wrap(namespace.getBytes(StandardCharsets.UTF_8));
			int size = udfav.write(XMLNS_ + prefix, bb);
			prefixes.put(prefix, namespace);
		}
	}

	boolean isRoot(Path path) {
		try {
			return isRoot && Files.isSameFile(rootPath, path);
		} catch (IOException e) {
			throw new ContentResourceException(e);
		}
	}

	@Override
	public Content get(ProvidedSession session, String mountPath, String relativePath) {
		return new FsContent(session, this, rootPath.resolve(relativePath));
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
		return NamespaceUtils.getPrefixes((ns) -> prefixes.entrySet().stream().filter(e -> e.getValue().equals(ns))
				.map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet()), namespaceURI);
	}

}
