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

import static org.argeo.eclipse.ui.utils.SingleSourcingConstants.FILE_SCHEME;
import static org.argeo.eclipse.ui.utils.SingleSourcingConstants.JCR_SCHEME;
import static org.argeo.eclipse.ui.utils.SingleSourcingConstants.SCHEME_HOST_SEPARATOR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.utils.SingleSourcingConstants;
import org.eclipse.rap.rwt.service.ServiceHandler;

/**
 * RWT specific Basic Default service handler that retrieves a file on the
 * server file system using its absolute path and forwards it to the end user
 * browser.
 * 
 * Clients might extend to provide context specific services
 */
public class OpenFileService implements ServiceHandler {
	public OpenFileService() {
	}

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String fileName = request.getParameter(SingleSourcingConstants.PARAM_FILE_NAME);
		String uri = request.getParameter(SingleSourcingConstants.PARAM_FILE_URI);

		// Set the Metadata
		response.setContentLength((int) getFileSize(uri));
		if (fileName == null || "".equals(fileName.trim()))
			fileName = getFileName(uri);
		response.setContentType(getMimeType(uri, fileName));
		String contentDisposition = "attachment; filename=\"" + fileName + "\"";
		response.setHeader("Content-Disposition", contentDisposition);

		// Useless for current use
		// response.setHeader("Content-Transfer-Encoding", "binary");
		// response.setHeader("Pragma", "no-cache");
		// response.setHeader("Cache-Control", "no-cache, must-revalidate");

		// Use buffered array to directly write the stream?
		response.getOutputStream().write(getFileAsByteArray(uri));
	}

	/**
	 * Retrieves the data as Byte Array given an uri.
	 * 
	 * Overwrite to provide application specific behaviours, like opening from a
	 * JCR repository
	 */
	protected byte[] getFileAsByteArray(String uri) {
		try {
			if (uri.startsWith(SingleSourcingConstants.FILE_SCHEME)) {
				Path path = Paths.get(getAbsPathFromUri(uri));
				return Files.readAllBytes(path);
			}
			// else if (uri.startsWith(JCR_SCHEME)) {
			// String absPath = Paths.get(getAbsPathFromUri(uri));
			// return Files.readAllBytes(path);
			// }

		} catch (IOException ioe) {
			throw new SingleSourcingException("Error getting the file at " + uri, ioe);
		}
		return null;
	}

	protected long getFileSize(String uri) throws IOException {
		if (uri.startsWith(SingleSourcingConstants.FILE_SCHEME)) {
			Path path = Paths.get(getAbsPathFromUri(uri));
			return Files.size(path);
		}
		return -1l;
	}

	protected String getFileName(String uri) {
		if (uri.startsWith(SingleSourcingConstants.FILE_SCHEME)) {
			Path path = Paths.get(getAbsPathFromUri(uri));
			return path.getFileName().toString();
		}
		return null;
	}

	private String getAbsPathFromUri(String uri) {
		if (uri.startsWith(FILE_SCHEME))
			return uri.substring((FILE_SCHEME + SCHEME_HOST_SEPARATOR).length());
		else if (uri.startsWith(JCR_SCHEME))
			return uri.substring((JCR_SCHEME + SCHEME_HOST_SEPARATOR).length());
		else
			throw new SingleSourcingException("Unknown URI prefix for" + uri);
	}

	protected String getMimeType(String uri, String fileName) throws IOException {
		if (uri.startsWith(FILE_SCHEME)) {
			Path path = Paths.get(getAbsPathFromUri(uri));
			String mimeType = Files.probeContentType(path);
			if (EclipseUiUtils.notEmpty(mimeType))
				return mimeType;
		}
		return getMimeTypeFromName(fileName);
	}

	/** Overwrite to precise the content type */
	protected String getMimeTypeFromName(String fileName) {
		return "application/octet-stream";
	}
}
