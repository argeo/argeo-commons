package org.argeo.jcr.fs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.argeo.jcr.JcrUtils;

/** A {@link FileStore} implementation based on JCR {@link Workspace}. */
public class WorkspaceFileStore extends FileStore {
	private final String mountPath;
	private final Workspace workspace;
	private final int mountDepth;

	public WorkspaceFileStore(String mountPath, Workspace workspace) {
		if ("/".equals(mountPath) || "".equals(mountPath))
			throw new IllegalArgumentException(
					"Mount path '" + mountPath + "' is unsupported, use null for the base file store");
		if (mountPath != null && !mountPath.startsWith(JcrPath.separator))
			throw new IllegalArgumentException("Mount path '" + mountPath + "' cannot end with /");
		if (mountPath != null && mountPath.endsWith(JcrPath.separator))
			throw new IllegalArgumentException("Mount path '" + mountPath + "' cannot end with /");
		this.mountPath = mountPath;
		if (mountPath == null)
			mountDepth = 0;
		else {
			mountDepth = mountPath.split(JcrPath.separator).length - 1;
		}
		this.workspace = workspace;
	}

	public void close() {
		JcrUtils.logoutQuietly(workspace.getSession());
	}

	@Override
	public String name() {
		return workspace.getName();
	}

	@Override
	public String type() {
		return "workspace";
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public long getTotalSpace() throws IOException {
		return 0;
	}

	@Override
	public long getUsableSpace() throws IOException {
		return 0;
	}

	@Override
	public long getUnallocatedSpace() throws IOException {
		return 0;
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		return false;
	}

	@Override
	public boolean supportsFileAttributeView(String name) {
		return false;
	}

	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
		return null;
	}

	@Override
	public Object getAttribute(String attribute) throws IOException {
		return workspace.getSession().getRepository().getDescriptor(attribute);
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public String toFsPath(Node node) throws RepositoryException {
		String nodeWorkspaceName = node.getSession().getWorkspace().getName();
		if (!nodeWorkspaceName.equals(workspace.getName()))
			throw new IllegalArgumentException("Icompatible " + node + " from workspace '" + nodeWorkspaceName
					+ "' in file store '" + workspace.getName() + "'");
		return mountPath == null ? node.getPath() : mountPath + node.getPath();
	}

	public boolean isBase() {
		return mountPath == null;
	}

	Node toNode(String[] fullPath) throws RepositoryException {
		String jcrPath = toJcrPath(fullPath);
		Session session = workspace.getSession();
		if (!session.itemExists(jcrPath))
			return null;
		Node node = session.getNode(jcrPath);
		return node;
	}

	private String toJcrPath(String[] path) {
		if (path == null)
			return "/";
		if (path.length < mountDepth)
			throw new IllegalArgumentException(
					"Path " + Arrays.asList(path) + " is no compatible with mount " + mountPath);

		if (!isBase()) {
			// check mount compatibility
			StringBuilder mount = new StringBuilder();
			mount.append('/');
			for (int i = 0; i < mountDepth; i++) {
				if (i != 0)
					mount.append('/');
				mount.append(Text.escapeIllegalJcrChars(path[i]));
			}
			if (!mountPath.equals(mount.toString()))
				throw new IllegalArgumentException(
						"Path " + Arrays.asList(path) + " is no compatible with mount " + mountPath);
		}

		StringBuilder sb = new StringBuilder();
		sb.append('/');
		for (int i = mountDepth; i < path.length; i++) {
			if (i != mountDepth)
				sb.append('/');
			sb.append(Text.escapeIllegalJcrChars(path[i]));
		}
		return sb.toString();
	}

}
