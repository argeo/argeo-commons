package org.argeo.cms.acr;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.spi.ContentProvider;

/** Manages the structural and dynamic mounts within the content repository. */
class MountManager {
	private final NavigableMap<String, ContentProvider> partitions = new TreeMap<>();

	private final CmsContentSession systemSession;

	public MountManager(CmsContentSession systemSession) {
		this.systemSession = systemSession;
	}

	synchronized void addStructuralContentProvider(ContentProvider contentProvider) {
		String mountPath = contentProvider.getMountPath();
		Objects.requireNonNull(mountPath);
		if (partitions.containsKey(mountPath))
			throw new IllegalStateException("A provider is already registered for " + mountPath);
		partitions.put(mountPath, contentProvider);
		if ("/".equals(mountPath))// root
			return;
		String[] parentPath = ContentUtils.getParentPath(mountPath);
		Content parent = systemSession.get(parentPath[0]);
		Content mount = parent.add(parentPath[1]);
		mount.put(CrName.mount.qName(), "true");

	}

	synchronized ContentProvider getOrAddMountedProvider(String mountPath, Function<String, ContentProvider> factory) {
		Objects.requireNonNull(factory);
		if (!partitions.containsKey(mountPath)) {
			ContentProvider contentProvider = factory.apply(mountPath);
			if (!mountPath.equals(contentProvider.getMountPath()))
				throw new IllegalArgumentException("Mount path " + mountPath + " is inconsistent with content provider "
						+ contentProvider.getMountPath());
			partitions.put(mountPath, contentProvider);
		}
		return partitions.get(mountPath);
	}

	/** The content provider for this path. */
	synchronized ContentProvider findContentProvider(String path) {
//		if (ContentUtils.EMPTY.equals(path))
//			return partitions.firstEntry().getValue();
		Map.Entry<String, ContentProvider> floorEntry = partitions.floorEntry(path);
		if (floorEntry == null)
			throw new IllegalArgumentException("No floor entry provider found for path '" + path + "'");
		String mountPath = floorEntry.getKey();
		if (!path.startsWith(mountPath)) {
			// FIXME make it more robust and find when there is no content provider
			String[] parent = ContentUtils.getParentPath(path);
			return findContentProvider(parent[0]);
			// throw new IllegalArgumentException("Path " + path + " doesn't have a content
			// provider");
		}
		ContentProvider contentProvider = floorEntry.getValue();
		assert mountPath.equals(contentProvider.getMountPath());
		return contentProvider;
	}

	/** All content providers under this path. */
	synchronized NavigableMap<String, ContentProvider> findContentProviders(String path) {
		Map.Entry<String, ContentProvider> floorEntry = partitions.floorEntry(path);
		if (floorEntry == null)
			throw new IllegalArgumentException("No floor entry provider found for path '" + path + "'");
		// we first find the parent provider
		String parentProviderPath = floorEntry.getKey();
		// then gather all sub-providers
		NavigableMap<String, ContentProvider> res = new TreeMap<>();
		res.put(floorEntry.getKey(), floorEntry.getValue());
		tail: for (Map.Entry<String, ContentProvider> provider : partitions.tailMap(parentProviderPath).entrySet()) {
			if (!provider.getKey().startsWith(path))
				break tail;
			res.put(provider.getKey(), provider.getValue());
		}
		return res;

	}

}
