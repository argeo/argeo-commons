/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.argeo.jcr;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;

/**
 * Bridge Spring resources and JCR folder / files semantics (nt:folder /
 * nt:file), supporting versioning as well.
 */
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
		JcrUtils.mkdirs(session(), path, NodeType.NT_FOLDER,
				NodeType.NT_FOLDER, versioning);
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

			Node contentNode = fileNode.addNode(Property.JCR_CONTENT,
					"nt:resource");
			if (mimeType != null)
				contentNode.setProperty(Property.JCR_MIMETYPE, mimeType);
			contentNode.setProperty(Property.JCR_ENCODING, defaultEncoding);
			Binary binary = session().getValueFactory().createBinary(in);
			contentNode.setProperty(Property.JCR_DATA, binary);
			JcrUtils.closeQuietly(binary);
			Calendar lastModified = Calendar.getInstance();
			// lastModified.setTimeInMillis(file.lastModified());
			contentNode.setProperty(Property.JCR_LAST_MODIFIED, lastModified);
			// resNode.addMixin("mix:referenceable");

			if (versioning)
				fileNode.addMixin("mix:versionable");

			session().save();

			if (versioning)
				session().getWorkspace().getVersionManager()
						.checkin(fileNode.getPath());

			if (log.isDebugEnabled())
				log.debug("Created " + path);
		} catch (Exception e) {
			throw new ArgeoException("Cannot create node for " + path, e);
		}

	}

	public void update(String path, InputStream in) {
		try {

			if (!session().itemExists(path)) {
				String type = null;
				// FIXME: using javax.activation leads to conflict between Java
				// 1.5 and 1.6 (since javax.activation was included in Java 1.6)
				// String type = new MimetypesFileTypeMap()
				// .getContentType(FilenameUtils.getName(path));
				create(path, in, type);
				return;
			}

			Node fileNode = (Node) session().getItem(path);
			Node contentNode = fileNode.getNode(Property.JCR_CONTENT);
			if (versioning)
				session().getWorkspace().getVersionManager()
						.checkout(fileNode.getPath());
			Binary binary = session().getValueFactory().createBinary(in);
			contentNode.setProperty(Property.JCR_DATA, binary);
			JcrUtils.closeQuietly(binary);
			Calendar lastModified = Calendar.getInstance();
			// lastModified.setTimeInMillis(file.lastModified());
			contentNode.setProperty(Property.JCR_LAST_MODIFIED, lastModified);

			session().save();
			if (versioning)
				session().getWorkspace().getVersionManager()
						.checkin(fileNode.getPath());

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
			VersionHistory history = session().getWorkspace()
					.getVersionManager().getVersionHistory(fileNode.getPath());
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
			Node node = (Node) session().getItem(
					path + "/" + Property.JCR_CONTENT);
			Property property = node.getProperty(Property.JCR_DATA);
			return property.getBinary().getStream();
		} catch (Exception e) {
			throw new ArgeoException("Cannot retrieve " + path, e);
		}
	}

	public synchronized InputStream retrieve(String path, Integer revision) {
		if (!versioning)
			throw new ArgeoException("Versioning is not activated");

		try {
			Node fileNode = (Node) session().getItem(path);
			VersionHistory history = session().getWorkspace()
					.getVersionManager().getVersionHistory(fileNode.getPath());
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
		InputStream in = frozenNode.getNode(Property.JCR_CONTENT)
				.getProperty(Property.JCR_DATA).getBinary().getStream();
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
