package org.argeo.jcr.fs;

import java.nio.file.attribute.FileTime;
import java.time.Instant;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.jcr.JcrUtils;

public class JcrBasicfileAttributes implements NodeFileAttributes {
	private final Node node;

	private FileTime EPOCH = FileTime.fromMillis(0);

	public JcrBasicfileAttributes(Node node) {
		this.node = node;
	}

	@Override
	public FileTime lastModifiedTime() {
		try {
			if (node.isNodeType(NodeType.MIX_LAST_MODIFIED)) {
				Instant instant = node.getProperty(Property.JCR_LAST_MODIFIED).getDate().toInstant();
				return FileTime.from(instant);
			}
			return EPOCH;
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot get last modified time", e);
		}
	}

	@Override
	public FileTime lastAccessTime() {
		return lastModifiedTime();
	}

	@Override
	public FileTime creationTime() {
		try {
			if (node.isNodeType(NodeType.MIX_CREATED)) {
				Instant instant = node.getProperty(Property.JCR_CREATED).getDate().toInstant();
				return FileTime.from(instant);
			}
			return EPOCH;
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot get creation time", e);
		}
	}

	@Override
	public boolean isRegularFile() {
		try {
			return node.isNodeType(NodeType.NT_FILE);
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot check if regular file", e);
		}
	}

	@Override
	public boolean isDirectory() {
		try {
			return node.isNodeType(NodeType.NT_FOLDER);
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot check if directory", e);
		}
	}

	@Override
	public boolean isSymbolicLink() {
		try {
			return node.isNodeType(NodeType.NT_LINKED_FILE);
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot check if linked file", e);
		}
	}

	@Override
	public boolean isOther() {
		return !(isDirectory() || isRegularFile() || isSymbolicLink());
	}

	@Override
	public long size() {
		if (isRegularFile()) {
			Binary binary = null;
			try {
				binary = node.getNode(Property.JCR_DATA).getProperty(Property.JCR_CONTENT).getBinary();
				return binary.getSize();
			} catch (RepositoryException e) {
				throw new JcrFsException("Cannot check size", e);
			} finally {
				JcrUtils.closeQuietly(binary);
			}
		}
		return -1;
	}

	@Override
	public Object fileKey() {
		return null;
	}

	@Override
	public Node getNode() {
		return node;
	}

}
