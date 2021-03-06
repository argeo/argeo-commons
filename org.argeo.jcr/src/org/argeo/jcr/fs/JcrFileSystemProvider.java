package org.argeo.jcr.fs;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.argeo.jcr.JcrUtils;

/** Operations on a {@link JcrFileSystem}. */
public abstract class JcrFileSystemProvider extends FileSystemProvider {

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		Node node = toNode(path);
		try {
			if (node == null) {
				Node parent = toNode(path.getParent());
				if (parent == null)
					throw new IOException("No parent directory for " + path);
				if (parent.getPrimaryNodeType().isNodeType(NodeType.NT_FILE)
						|| parent.getPrimaryNodeType().isNodeType(NodeType.NT_LINKED_FILE))
					throw new IOException(path + " parent is a file");

				String fileName = path.getFileName().toString();
				fileName = Text.escapeIllegalJcrChars(fileName);
				node = parent.addNode(fileName, NodeType.NT_FILE);
				node.addMixin(NodeType.MIX_CREATED);
//				node.addMixin(NodeType.MIX_LAST_MODIFIED);
			}
			if (!node.isNodeType(NodeType.NT_FILE))
				throw new UnsupportedOperationException(node + " must be a file");
			return new BinaryChannel(node, path);
		} catch (RepositoryException e) {
			discardChanges(node);
			throw new IOException("Cannot read file", e);
		}
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		try {
			Node base = toNode(dir);
			if (base == null)
				throw new IOException(dir + " is not a JCR node");
			JcrFileSystem fileSystem = (JcrFileSystem) dir.getFileSystem();
			return new NodeDirectoryStream(fileSystem, base.getNodes(), fileSystem.listDirectMounts(dir), filter);
		} catch (RepositoryException e) {
			throw new IOException("Cannot list directory", e);
		}
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		Node node = toNode(dir);
		try {
			if (node == null) {
				Node parent = toNode(dir.getParent());
				if (parent == null)
					throw new IOException("Parent of " + dir + " does not exist");
				Session session = parent.getSession();
				synchronized (session) {
					if (parent.getPrimaryNodeType().isNodeType(NodeType.NT_FILE)
							|| parent.getPrimaryNodeType().isNodeType(NodeType.NT_LINKED_FILE))
						throw new IOException(dir + " parent is a file");
					String fileName = dir.getFileName().toString();
					fileName = Text.escapeIllegalJcrChars(fileName);
					node = parent.addNode(fileName, NodeType.NT_FOLDER);
					node.addMixin(NodeType.MIX_CREATED);
					node.addMixin(NodeType.MIX_LAST_MODIFIED);
					save(session);
				}
			} else {
				// if (!node.getPrimaryNodeType().isNodeType(NodeType.NT_FOLDER))
				// throw new FileExistsException(dir + " exists and is not a directory");
			}
		} catch (RepositoryException e) {
			discardChanges(node);
			throw new IOException("Cannot create directory " + dir, e);
		}
	}

	@Override
	public void delete(Path path) throws IOException {
		Node node = toNode(path);
		try {
			if (node == null)
				throw new NoSuchFileException(path + " does not exist");
			Session session = node.getSession();
			synchronized (session) {
				session.refresh(false);
				if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FILE))
					node.remove();
				else if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FOLDER)) {
					if (node.hasNodes())// TODO check only files
						throw new DirectoryNotEmptyException(path.toString());
					node.remove();
				}
				save(session);
			}
		} catch (RepositoryException e) {
			discardChanges(node);
			throw new IOException("Cannot delete " + path, e);
		}

	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		Node sourceNode = toNode(source);
		Node targetNode = toNode(target);
		try {
			Session targetSession = targetNode.getSession();
			synchronized (targetSession) {
				JcrUtils.copy(sourceNode, targetNode);
				save(targetSession);
			}
		} catch (RepositoryException e) {
			discardChanges(sourceNode);
			discardChanges(targetNode);
			throw new IOException("Cannot copy from " + source + " to " + target, e);
		}
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		JcrFileSystem sourceFileSystem = (JcrFileSystem) source.getFileSystem();
		WorkspaceFileStore sourceStore = sourceFileSystem.getFileStore(source.toString());
		WorkspaceFileStore targetStore = sourceFileSystem.getFileStore(target.toString());
		try {
			if (sourceStore.equals(targetStore)) {
				sourceStore.getWorkspace().move(sourceStore.toJcrPath(source.toString()),
						targetStore.toJcrPath(target.toString()));
			} else {
				// TODO implement it
				throw new UnsupportedOperationException("Can only move paths within the same workspace.");
			}
		} catch (RepositoryException e) {
			throw new IOException("Cannot move from " + source + " to " + target, e);
		}

//		Node sourceNode = toNode(source);
//		try {
//			Session session = sourceNode.getSession();
//			synchronized (session) {
//				session.move(sourceNode.getPath(), target.toString());
//				save(session);
//			}
//		} catch (RepositoryException e) {
//			discardChanges(sourceNode);
//			throw new IOException("Cannot move from " + source + " to " + target, e);
//		}
	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		if (path.getFileSystem() != path2.getFileSystem())
			return false;
		boolean equ = path.equals(path2);
		if (equ)
			return true;
		else {
			try {
				Node node = toNode(path);
				Node node2 = toNode(path2);
				return node.isSame(node2);
			} catch (RepositoryException e) {
				throw new IOException("Cannot check whether " + path + " and " + path2 + " are same", e);
			}
		}

	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		return path.getFileName().toString().charAt(0) == '.';
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		JcrFileSystem fileSystem = (JcrFileSystem) path.getFileSystem();
		return fileSystem.getFileStore(path.toString());
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		Node node = toNode(path);
		if (node == null)
			throw new NoSuchFileException(path + " does not exist");
		// TODO check access via JCR api
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
			throws IOException {
		// TODO check if assignable
		Node node = toNode(path);
		if (node == null) {
			throw new IOException("JCR node not found for " + path);
		}
		return (A) new JcrBasicfileAttributes(node);
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		try {
			Node node = toNode(path);
			String pattern = attributes.replace(',', '|');
			Map<String, Object> res = new HashMap<String, Object>();
			PropertyIterator it = node.getProperties(pattern);
			props: while (it.hasNext()) {
				Property prop = it.nextProperty();
				PropertyDefinition pd = prop.getDefinition();
				if (pd.isMultiple())
					continue props;
				int requiredType = pd.getRequiredType();
				switch (requiredType) {
				case PropertyType.LONG:
					res.put(prop.getName(), prop.getLong());
					break;
				case PropertyType.DOUBLE:
					res.put(prop.getName(), prop.getDouble());
					break;
				case PropertyType.BOOLEAN:
					res.put(prop.getName(), prop.getBoolean());
					break;
				case PropertyType.DATE:
					res.put(prop.getName(), prop.getDate());
					break;
				case PropertyType.BINARY:
					byte[] arr = JcrUtils.getBinaryAsBytes(prop);
					res.put(prop.getName(), arr);
					break;
				default:
					res.put(prop.getName(), prop.getString());
				}
			}
			return res;
		} catch (RepositoryException e) {
			throw new IOException("Cannot read attributes of " + path, e);
		}
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		Node node = toNode(path);
		try {
			Session session = node.getSession();
			synchronized (session) {
				if (value instanceof byte[]) {
					JcrUtils.setBinaryAsBytes(node, attribute, (byte[]) value);
				} else if (value instanceof Calendar) {
					node.setProperty(attribute, (Calendar) value);
				} else {
					node.setProperty(attribute, value.toString());
				}
				save(session);
			}
		} catch (RepositoryException e) {
			discardChanges(node);
			throw new IOException("Cannot set attribute " + attribute + " on " + path, e);
		}
	}

	protected Node toNode(Path path) {
		try {
			return ((JcrPath) path).getNode();
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot convert path " + path + " to JCR Node", e);
		}
	}

	/** Discard changes in the underlying session */
	protected void discardChanges(Node node) {
		if (node == null)
			return;
		try {
			// discard changes
			node.getSession().refresh(false);
		} catch (RepositoryException e) {
			e.printStackTrace();
			// TODO log out session?
			// TODO use Commons logging?
		}
	}

	/** Make sure save is robust. */
	protected void save(Session session) throws RepositoryException {
		session.refresh(true);
		session.save();
		session.notifyAll();
	}

	/**
	 * To be overriden in order to support the ~ path, with an implementation
	 * specific concept of user home.
	 * 
	 * @return null by default
	 */
	public Node getUserHome(Repository session) {
		return null;
	}

}
