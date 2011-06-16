package org.argeo.eclipse.ui.specific;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.argeo.ArgeoException;
import org.eclipse.rwt.RWT;
import org.eclipse.rwt.service.IServiceHandler;

public class DownloadServiceHandler implements IServiceHandler {

	private FileProvider provider;

	public DownloadServiceHandler(FileProvider provider) {
		this.provider = provider;
	}

	public void service() throws IOException, ServletException {
		// Which file to download?
		String fileName = RWT.getRequest().getParameter("filename");
		String fileId = RWT.getRequest().getParameter("fileid");

		// Get the file content
		byte[] download = provider.getByteArrayFileFromId(fileId);

		// Send the file in the response
		HttpServletResponse response = RWT.getResponse();
		response.setContentType("application/octet-stream");
		response.setContentLength(download.length);
		String contentDisposition = "attachment; filename=\"" + fileName + "\"";
		response.setHeader("Content-Disposition", contentDisposition);
		// response.setHeader( "Cache-Control", "no-cache" );

		try {
			response.getOutputStream().write(download);
		} catch (IOException ioe) {
			throw new ArgeoException("Error while writing the file " + fileName
					+ " to the servlet response", ioe);
		}
	}
}
