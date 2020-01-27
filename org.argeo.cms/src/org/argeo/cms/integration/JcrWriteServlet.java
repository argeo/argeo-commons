package org.argeo.cms.integration;

import java.io.IOException;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jcr.JcrUtils;

/** Access a JCR repository via web services. */
public class JcrWriteServlet extends JcrReadServlet {
	private static final long serialVersionUID = 17272653843085492L;
	private final static Log log = LogFactory.getLog(JcrWriteServlet.class);

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (log.isTraceEnabled())
			log.trace("Data service: " + req.getPathInfo());

		String dataWorkspace = getWorkspace(req);
		String jcrPath = getJcrPath(req);

		Session session = null;
		try {
			// authentication
			session = openJcrSession(req, resp, getRepository(), dataWorkspace);

			if (req.getContentType() != null && req.getContentType().equals(XML_CONTENT_TYPE)) {
//				resp.setContentType(XML_CONTENT_TYPE);
				session.getWorkspace().importXML(jcrPath, req.getInputStream(),
						ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
				return;
			}

			if (!session.itemExists(jcrPath)) {
				String parentPath = FilenameUtils.getFullPathNoEndSeparator(jcrPath);
				String fileName = FilenameUtils.getName(jcrPath);
				Node folderNode = JcrUtils.mkfolders(session, parentPath);
				byte[] bytes = IOUtils.toByteArray(req.getInputStream());
				JcrUtils.copyBytesAsFile(folderNode, fileName, bytes);
			} else {
				Node node = session.getNode(jcrPath);
				if (!node.isNodeType(NodeType.NT_FILE))
					throw new IllegalArgumentException("Node " + jcrPath + " exists but is not a file");
				byte[] bytes = IOUtils.toByteArray(req.getInputStream());
				JcrUtils.copyBytesAsFile(node.getParent(), node.getName(), bytes);
			}
		} catch (Exception e) {
			new CmsExceptionsChain(e).writeAsJson(getObjectMapper(), resp);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}
}
