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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.io.FileExistsException;
import org.argeo.jcr.JcrUtils;

public abstract class JcrFileSystemProvider extends FileSystemProvider {
	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		try {
			Node node = toNode(path);
			if (node == null) {
				Node parent = toNode(path.getParent());
				if (parent == null)
					throw new JcrFsException("No parent directory for " + path);
				if (parent.getPrimaryNodeType().isNodeType(NodeType.NT_FILE)
						|| parent.getPrimaryNodeType().isNodeType(NodeType.NT_LINKED_FILE))
					throw new JcrFsException(path + " parent is a file");

				String fileName = path.getFileName().toString();
				fileName = Text.escapeIllegalJcrChars(fileName);
				node = parent.addNode(fileName, NodeType.NT_FILE);
				node.addMixin(NodeType.MIX_CREATED);
				node.addMixin(NodeType.MIX_LAST_MODIFIED);
			}
			if (!node.isNodeType(NodeType.NT_FILE))
				throw new UnsupportedOperationException(node + " must be a file");
			return new BinaryChannel(node);
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot read file", e);
		}
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		try {
			Node base = toNode(dir);
			return new NodeDirectoryStream((JcrFileSystem) dir.getFileSystem(), base.getNodes(), filter);
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot list directory", e);
		}
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		try {
			Node node = toNode(dir);
			if (node == null) {
				Node parent = toNode(dir.getParent());
				if (parent == null)
					throw new IOException("Parent of " + dir + " does not exist");
				if (parent.getPrimaryNodeType().isNodeType(NodeType.NT_FILE)
						|| parent.getPrimaryNodeType().isNodeType(NodeType.NT_LINKED_FILE))
					throw new JcrFsException(dir + " parent is a file");
				String fileName = dir.getFileName().toString();
				fileName = Text.escapeIllegalJcrChars(fileName);
				node = parent.addNode(fileName, NodeType.NT_FOLDER);
				node.addMixin(NodeType.MIX_CREATED);
				node.addMixin(NodeType.MIX_LAST_MODIFIED);
				node.getSession().save();
			} else {
				if (!node.getPrimaryNodeType().isNodeType(NodeType.NT_FOLDER))
					throw new FileExistsException(dir + " exists and is not a directory");
			}
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot create directory " + dir, e);
		}

	}

	@Override
	public void delete(Path path) throws IOException {
		try {
			Node node = toNode(path);
			if (node == null)
				throw new NoSuchFileException(path + " does not exist");
			Session session = node.getSession();
			if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FILE))
				node.remove();
			else if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FOLDER)) {
				if (node.hasNodes())// TODO check only files
					throw new DirectoryNotEmptyException(path.toString());
				node.remove();
			}
			session.save();
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot delete " + path, e);
		}

	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		try {
			Node sourceNode = toNode(source);
			Node targetNode = toNode(target);
			JcrUtils.copy(sourceNode, targetNode);
			sourceNode.getSession().save();
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot copy from " + source + " to " + target, e);
		}
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		try {
			Node sourceNode = toNode(source);
			Session session = sourceNode.getSession();
			session.move(sourceNode.getPath(), target.toString());
			session.save();
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot move from " + source + " to " + target, e);
		}
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
				throw new JcrFsException("Cannot check whether " + path + " and " + path2 + " are same", e);
			}
		}

	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		return false;
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		Session session = ((JcrFileSystem) path.getFileSystem()).getSession();
		return new WorkSpaceFileStore(session.getWorkspace());
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		try {
			Session session = ((JcrFileSystem) path.getFileSystem()).getSession();
			if (!session.itemExists(path.toString()))
				throw new NoSuchFileException(path + " does not exist");
			// TODO check access via JCR api
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot delete " + path, e);
		}
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
			throws IOException {
		try {
			// TODO check if assignable
			Node node = toNode(path);
			if(node==null) {
				throw new JcrFsException("JCR node not found for "+path);
			}
			return (A) new JcrBasicfileAttributes(node);
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot read basic attributes of " + path, e);
		}
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
			throw new JcrFsException("Cannot read attributes of " + path, e);
		}
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		try {
			Node node = toNode(path);
			if (value instanceof byte[]) {
				JcrUtils.setBinaryAsBytes(node, attribute, (byte[]) value);
			} else if (value instanceof Calendar) {
				node.setProperty(attribute, (Calendar) value);
			} else {
				node.setProperty(attribute, value.toString());
			}
			node.getSession().save();
		} catch (RepositoryException e) {
			throw new JcrFsException("Cannot set attribute " + attribute + " on " + path, e);
		}
	}

	protected Node toNode(Path path) throws RepositoryException {
		return ((JcrPath) path).getNode();
	}

	/**
	 * To be overriden in order to support the ~ path, with an implementation
	 * specific concept of user home.
	 * 
	 * @return null by default
	 */
	public Node getUserHome(Session session) {
		return null;
	}
}
