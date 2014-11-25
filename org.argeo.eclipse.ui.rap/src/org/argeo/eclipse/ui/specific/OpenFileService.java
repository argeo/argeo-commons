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

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.argeo.ArgeoException;
import org.eclipse.rap.rwt.service.ServiceHandler;

/**
 * Basic Default service handler that retrieves a file on the server file system
 * using its absolute path and forwards it to the end user browser. Rap
 * specific.
 * 
 * Clients might extend to provide context specific services (to open files from
 * a JCR repository for instance)
 */
public class OpenFileService implements ServiceHandler {
	public final static String PARAM_FILE_NAME = "param.fileName";
	public final static String PARAM_FILE_URI = "param.fileURI";

	public final static String SCHEME_HOST_SEPARATOR = "://";
	public final static String FILE_SCHEME = "file";

	public OpenFileService() {
	}

	public void service(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String fileName = request.getParameter(PARAM_FILE_NAME);
		String uri = request.getParameter(PARAM_FILE_URI);

		// Set the Metadata
		response.setContentType("application/octet-stream");
		response.setContentLength((int) getFileLength(uri));
		if (fileName == null || "".equals(fileName.trim()))
			fileName = getFileName(uri);
		String contentDisposition = "attachment; filename=\"" + fileName + "\"";
		response.setHeader("Content-Disposition", contentDisposition);

		response.getOutputStream().write(getFileAsByteArray(uri));
		// FileUtils.readFileToByteArray(new File(path))
	}

	protected byte[] getFileAsByteArray(String uri) {
		if (uri.startsWith(FILE_SCHEME)) {
			try {
				return FileUtils.readFileToByteArray(new File(
						getFilePathFromUri(uri)));
			} catch (IOException ioe) {
				throw new ArgeoException("Error while getting the file at "
						+ uri, ioe);
			}
		}
		return null;
	}

	protected long getFileLength(String uri) {
		if (uri.startsWith(FILE_SCHEME)) {
			return new File(getFilePathFromUri(uri)).length();
		}
		return -1l;
	}

	protected String getFileName(String uri) {
		if (uri.startsWith(FILE_SCHEME)) {
			return new File(getFilePathFromUri(uri)).getName();
		}
		return null;
	}

	private String getFilePathFromUri(String uri) {
		return uri.substring((FILE_SCHEME + SCHEME_HOST_SEPARATOR).length());
	}

}