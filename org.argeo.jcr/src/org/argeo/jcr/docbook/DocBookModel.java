package org.argeo.jcr.docbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DocBookModel {
	private final static Log log = LogFactory.getLog(DocBookModel.class);
	private Session session;

	public DocBookModel(Session session) {
		super();
		this.session = session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void importXml(String path, InputStream in)
			throws RepositoryException, IOException {
		long begin = System.currentTimeMillis();
		session.importXML(path, in,
				ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
		long duration = System.currentTimeMillis() - begin;
		if (log.isTraceEnabled())
			log.trace("Imported " + path + " in " + duration + " ms");

	}
	
	public void exportXml(String path, OutputStream out)
			throws RepositoryException, IOException {
		session.exportDocumentView(path, out, true, false);
	}
}
