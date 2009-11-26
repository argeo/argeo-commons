package org.argeo.server.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;

import javax.activation.MimetypesFileTypeMap;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

public class JcrResourceAdapter implements InitializingBean, DisposableBean {
	private final static Log log = LogFactory.getLog(JcrResourceAdapter.class);

	private Repository repository;

	private String username;
	private String password;

	private Session session;

	private Boolean versioning = true;
	private String defaultEncoding = "UTF-8";

	// private String restoreBase = "/.restore";

	public void mkdirs(String path) {
		try {
			StringTokenizer st = new StringTokenizer(path, "/");
			StringBuffer current = new StringBuffer("/");
			Node currentNode = session().getRootNode();
			while (st.hasMoreTokens()) {
				String part = st.nextToken();
				current.append(part).append('/');
				if (!session().itemExists(current.toString())) {
					currentNode = currentNode.addNode(part, "nt:folder");
					if (versioning)
						currentNode.addMixin("mix:versionable");
					if (log.isTraceEnabled())
						log.debug("Added folder " + part + " as " + current);
				} else {
					currentNode = (Node) session().getItem(current.toString());
				}
			}
			session().save();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot mkdirs " + path, e);
		}
	}

	public void create(String path, Resource file, String mimeType) {
		try {
			create(path, file.getInputStream(), mimeType);
		} catch (IOException e) {
			throw new ArgeoException("Cannot read " + file, e);
		}
	}

	public void create(String path, InputStream in, String mimeType) {
		try {
			if (session().itemExists(path)) {
				throw new ArgeoException("Node " + path + " already exists.");
			}

			int index = path.lastIndexOf('/');
			String parentPath = path.substring(0, index);
			if (parentPath.equals(""))
				parentPath = "/";
			String fileName = path.substring(index + 1);
			if (!session().itemExists(parentPath))
				throw new ArgeoException("Parent folder of node " + path
						+ " does not exist: " + parentPath);

			Node folderNode = (Node) session().getItem(parentPath);
			Node fileNode = folderNode.addNode(fileName, "nt:file");

			Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
			if (mimeType != null)
				contentNode.setProperty("jcr:mimeType", mimeType);
			contentNode.setProperty("jcr:encoding", defaultEncoding);
			contentNode.setProperty("jcr:data", in);
			Calendar lastModified = Calendar.getInstance();
			// lastModified.setTimeInMillis(file.lastModified());
			contentNode.setProperty("jcr:lastModified", lastModified);
			// resNode.addMixin("mix:referenceable");

			if (versioning)
				fileNode.addMixin("mix:versionable");

			session().save();

			if (versioning)
				fileNode.checkin();

			if (log.isDebugEnabled())
				log.debug("Created " + path);
		} catch (Exception e) {
			throw new ArgeoException("Cannot create node for " + path, e);
		}

	}

	public void update(String path, Resource file) {
		try {
			update(path, file.getInputStream());
		} catch (IOException e) {
			throw new ArgeoException("Cannot read " + file, e);
		}
	}

	public void update(String path, InputStream in) {
		try {

			if (!session().itemExists(path)) {
				String type = new MimetypesFileTypeMap()
						.getContentType(FilenameUtils.getName(path));
				create(path, in, type);
				return;
			}

			Node fileNode = (Node) session().getItem(path);
			Node contentNode = fileNode.getNode("jcr:content");
			fileNode.checkout();
			contentNode.setProperty("jcr:data", in);
			Calendar lastModified = Calendar.getInstance();
			// lastModified.setTimeInMillis(file.lastModified());
			contentNode.setProperty("jcr:lastModified", lastModified);

			session().save();
			fileNode.checkin();

			if (log.isDebugEnabled())
				log.debug("Updated " + path);
		} catch (Exception e) {
			throw new ArgeoException("Cannot update node " + path, e);
		}
	}

	public List<Calendar> listVersions(String path) {
		if (!versioning)
			throw new ArgeoException("Versioning is not activated");

		try {
			List<Calendar> versions = new ArrayList<Calendar>();
			Node fileNode = (Node) session().getItem(path);
			VersionHistory history = fileNode.getVersionHistory();
			for (VersionIterator it = history.getAllVersions(); it.hasNext();) {
				Version version = (Version) it.next();
				versions.add(version.getCreated());
				if (log.isTraceEnabled()) {
					log.debug(version);
					// debug(version);
				}
			}
			return versions;
		} catch (Exception e) {
			throw new ArgeoException("Cannot list version of node " + path, e);
		}
	}

	public InputStream retrieve(String path) {
		try {
			Node node = (Node) session().getItem(path + "/jcr:content");
			Property property = node.getProperty("jcr:data");
			return property.getStream();
		} catch (Exception e) {
			throw new ArgeoException("Cannot retrieve " + path, e);
		}
	}

	public synchronized InputStream retrieve(String path, Integer revision) {
		if (!versioning)
			throw new ArgeoException("Versioning is not activated");

		try {
			Node fileNode = (Node) session().getItem(path);

			// if (revision == 0) {
			// InputStream in = fromVersion(fileNode.getBaseVersion());
			// if (log.isDebugEnabled())
			// log.debug("Retrieved " + path + " at base revision ");
			// return in;
			// }

			VersionHistory history = fileNode.getVersionHistory();
			int count = 0;
			Version version = null;
			for (VersionIterator it = history.getAllVersions(); it.hasNext();) {
				version = (Version) it.next();
				if (count == revision + 1) {
					InputStream in = fromVersion(version);
					if (log.isDebugEnabled())
						log.debug("Retrieved " + path + " at revision "
								+ revision);
					return in;
				}
				count++;
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot retrieve version " + revision
					+ " of " + path, e);
		}

		throw new ArgeoException("Version " + revision
				+ " does not exist for node " + path);
	}

	protected InputStream fromVersion(Version version)
			throws RepositoryException {
		Node frozenNode = version.getNode("jcr:frozenNode");
		InputStream in = frozenNode.getNode("jcr:content").getProperty(
				"jcr:data").getStream();
		return in;
	}

	// protected InputStream restoreOrGetRevision(Node fileNode, Version
	// version,
	// Integer revision) throws RepositoryException {
	// Node parentFolder = (Node) fileNode
	// .getAncestor(fileNode.getDepth() - 1);
	// String restoreFolderPath = restoreBase + parentFolder.getPath();
	// mkdirs(restoreFolderPath);
	// String subNodeName = fileNode.getName() + "__" + fill(revision);
	// Node restoreFolder = (Node) session().getItem(restoreFolderPath);
	// if (!restoreFolder.hasNode(subNodeName)) {
	// parentFolder.restore(version, subNodeName, true);
	// }
	// return parentFolder.getNode(subNodeName + "/jcr:content").getProperty(
	// "jcr:data").getStream();
	// }

	protected Session session() {
		return session;
	}

	public void afterPropertiesSet() throws Exception {
		session = repository.login(new SimpleCredentials(username, password
				.toCharArray()));
	}

	public void destroy() throws Exception {
		session.logout();
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setVersioning(Boolean versioning) {
		this.versioning = versioning;
	}

	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/** Recursively outputs the contents of the given node. */
	public static void debug(Node node) throws RepositoryException {
		// First output the node path
		log.debug(node.getPath());
		// Skip the virtual (and large!) jcr:system subtree
		if (node.getName().equals("jcr:system")) {
			return;
		}

		// Then output the properties
		PropertyIterator properties = node.getProperties();
		while (properties.hasNext()) {
			Property property = properties.nextProperty();
			if (property.getDefinition().isMultiple()) {
				// A multi-valued property, print all values
				Value[] values = property.getValues();
				for (int i = 0; i < values.length; i++) {
					log.debug(property.getPath() + " = "
							+ values[i].getString());
				}
			} else {
				// A single-valued property
				log.debug(property.getPath() + " = " + property.getString());
			}
		}

	}

	protected String fill(Integer number) {
		int size = 4;
		String str = number.toString();
		for (int i = str.length(); i < size; i++) {
			str = "0" + str;
		}
		return str;
	}
}
