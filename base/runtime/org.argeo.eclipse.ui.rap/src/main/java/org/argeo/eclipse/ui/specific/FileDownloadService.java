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

/** Provide a basic handler that returns a file from the file system in Rap. */
public class FileDownloadService implements ServiceHandler {
	public final static String PARAM_FILE_NAME = "param.fileName";
	public final static String PARAM_FILE_PATH = "param.filePath";
	
	public FileDownloadService() {
	}

	public void service(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String fileName = request.getParameter(PARAM_FILE_NAME);
		String path = request.getParameter(PARAM_FILE_PATH);

		// Get the file
		File file = new File(path);

		// Send the Metadata
		response.setContentType("application/octet-stream");
		response.setContentLength((int) file.length());
		String contentDisposition = "attachment; filename=\"" + fileName + "\"";
		response.setHeader("Content-Disposition", contentDisposition);

		try {
			response.getOutputStream().write(
					FileUtils.readFileToByteArray(new File(path)));
		} catch (IOException ioe) {
			throw new ArgeoException("Error while writing the file " + fileName
					+ " to the servlet response", ioe);
		}
	}
}