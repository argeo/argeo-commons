package org.argeo.server.jcr.mvc;

import java.util.List;
import java.util.StringTokenizer;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jcr.JcrResourceAdapter;
import org.argeo.server.ServerAnswer;
import org.argeo.server.mvc.MvcConstants;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

@Controller
public class JcrManagerController implements MvcConstants, JcrMvcConstants {
	private final static Log log = LogFactory
			.getLog(JcrManagerController.class);

	// Create a factory for disk-based file items
	private FileItemFactory factory = new DiskFileItemFactory();

	// Create a new file upload handler
	private ServletFileUpload upload = new ServletFileUpload(factory);

	@SuppressWarnings("unchecked")
	@RequestMapping("/upload/**")
	@ModelAttribute(ANSWER_MODEL_KEY_AS_HTML)
	public ServerAnswer upload(WebRequest webRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		Session session = ((Session) webRequest.getAttribute(
				REQUEST_ATTR_SESSION, RequestAttributes.SCOPE_REQUEST));
		JcrResourceAdapter resourceAdapter = new JcrResourceAdapter(session);
		// Parse the request
		List<FileItem> items = upload.parseRequest(request);

		byte[] arr = null;
		for (FileItem item : items) {
			if (!item.isFormField()) {
				arr = item.get();
				break;
			}
		}

		ByteArrayResource res = new ByteArrayResource(arr);
		// String pathInfo = request.getPathInfo();

		StringBuffer path = new StringBuffer("/");
		StringTokenizer st = new StringTokenizer(request.getPathInfo(), "/");
		st.nextToken();// skip /upload/
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (!st.hasMoreTokens()) {
				resourceAdapter.mkdirs(path.toString());
				path.append(token);
			} else {
				path.append(token).append('/');
			}
		}
		// String path = '/' + pathInfo.substring(1).substring(
		// pathInfo.indexOf('/'));
		if (log.isDebugEnabled())
			log.debug("Upload to " + path);
		resourceAdapter.update(path.toString(), res);
		return ServerAnswer.ok("File " + path + " imported");
	}

}
