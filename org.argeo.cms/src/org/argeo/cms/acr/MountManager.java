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
		mount.put(CrName.MOUNT.get(), "true");

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

	synchronized ContentProvider findContentProvider(String path) {
		Map.Entry<String, ContentProvider> entry = partitions.floorEntry(path);
		if (entry == null)
			throw new IllegalArgumentException("No entry provider found for " + path);
		String mountPath = entry.getKey();
		ContentProvider contentProvider = entry.getValue();
		assert mountPath.equals(contentProvider.getMountPath());
		return contentProvider;
	}
}