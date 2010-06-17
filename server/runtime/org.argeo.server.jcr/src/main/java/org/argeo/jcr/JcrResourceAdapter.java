package org.argeo.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.springframework.core.io.Resource;

public class JcrResourceAdapter {
	private final static Log log = LogFactory.getLog(JcrResourceAdapter.class);

	private Session session;

	private Boolean versioning = true;
	private String defaultEncoding = "UTF-8";

	// private String restoreBase = "/.restore";

	public JcrResourceAdapter() {
	}

	public JcrResourceAdapter(Session session) {
		this.session = session;
	}

	public void mkdirs(String path) {
		JcrUtils.mkdirs(session(), path, "nt:folder", versioning);
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
			if (versioning)
				fileNode.checkout();
			contentNode.setProperty("jcr:data", in);
			Calendar lastModified = Calendar.getInstance();
			// lastModified.setTimeInMillis(file.lastModified());
			contentNode.setProperty("jcr:lastModified", lastModified);

			session().save();
			if (versioning)
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

	protected Session session() {
		return session;
	}

	public void setVersioning(Boolean versioning) {
		this.versioning = versioning;
	}

	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	protected String fill(Integer number) {
		int size = 4;
		String str = number.toString();
		for (int i = str.length(); i < size; i++) {
			str = "0" + str;
		}
		return str;
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
