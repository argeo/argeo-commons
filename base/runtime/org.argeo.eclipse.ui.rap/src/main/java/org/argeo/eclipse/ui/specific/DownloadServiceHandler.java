/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
package org.argeo.eclipse.ui.specific;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.argeo.ArgeoException;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.service.IServiceHandler;

public class DownloadServiceHandler implements IServiceHandler {

	private FileProvider provider;

	public DownloadServiceHandler(FileProvider provider) {
		this.provider = provider;
	}

	public void service( HttpServletRequest request, HttpServletResponse response )  throws IOException, ServletException {
		// Which file to download?
		String fileName = request.getParameter("filename");
		String fileId = request.getParameter("fileid");

		// Get the file content
		byte[] download = provider.getByteArrayFileFromId(fileId);

		// Send the file in the response
		response.setContentType("application/octet-stream");
		response.setContentLength(download.length);
		String contentDisposition = "attachment; filename=\"" + fileName + "\"";
		response.setHeader("Content-Disposition", contentDisposition);

		// Various header fields that can be set to solve some issues with some
		// old browsers.
		// Unused.
		// String contentType = "application/force-download; name=\"" + fileName
		// + "\"";
		// response.setContentType(contentType);
		// response.setHeader("Content-Transfer-Encoding", "binary");
		// response.setHeader("Pragma", "no-cache");
		// response.setHeader("Cache-Control", "no-cache, must-revalidate");
		// response.setHeader("Expires", "0");
		// response.setHeader("Connection", "Keep-Alive");
		// response.setHeader("Keep-Alive", "timeout=5, max=86");
		// response.setHeader("transfer-Encoding", "chunked");

		try {
			response.getOutputStream().write(download);
		} catch (IOException ioe) {
			throw new ArgeoException("Error while writing the file " + fileName
					+ " to the servlet response", ioe);
		}
	}
}
