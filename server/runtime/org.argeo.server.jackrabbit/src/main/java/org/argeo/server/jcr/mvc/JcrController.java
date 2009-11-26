package org.argeo.server.jcr.mvc;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.argeo.server.ServerAnswer;
import org.argeo.server.jcr.JcrResourceAdapter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Controller;

@Controller
public class JcrController {
	private JcrResourceAdapter resourceAdapter;

	// Create a factory for disk-based file items
	private FileItemFactory factory = new DiskFileItemFactory();

	// Create a new file upload handler
	private ServletFileUpload upload = new ServletFileUpload(factory);

	public ServerAnswer importFile(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
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
		return ServerAnswer.ok("File imported");
	}
}
