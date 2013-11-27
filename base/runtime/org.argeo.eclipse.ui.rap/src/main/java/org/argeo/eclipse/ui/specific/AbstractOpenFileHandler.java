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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.UrlLauncher;

/**
 * Rap specific handler to open a file stored in the server file system, among
 * other tmp files created for exports.
 * 
 */
public abstract class AbstractOpenFileHandler extends AbstractHandler {
	private final static Log log = LogFactory
			.getLog(AbstractOpenFileHandler.class);

	// Must be declared by implementing classes
	// public final static String ID = "org.argeo.eclipse.ui.specific.openFile";

	public final static String PARAM_FILE_NAME = FileDownloadServiceHandler.PARAM_FILE_NAME;
	public final static String PARAM_FILE_PATH = FileDownloadServiceHandler.PARAM_FILE_PATH;

	public Object execute(ExecutionEvent event) throws ExecutionException {

		// Try to register each time we execute the command.
		// try {
		// ServiceHandler handler = new FileDownloadServiceHandler();
		// RWT.getServiceManager().registerServiceHandler(
		// FileDownloadServiceHandler.DOWNLOAD_SERVICE_NAME, handler);
		// } catch (IllegalArgumentException iae) {
		// log.warn("Handler is already registered, clean this registering process");
		// }

		// The real usefull handler
		String fileName = event.getParameter(PARAM_FILE_NAME);
		String filePath = event.getParameter(PARAM_FILE_PATH);

		StringBuilder url = new StringBuilder();
		url.append("&").append(PARAM_FILE_NAME).append("=");
		url.append(fileName);
		url.append("&").append(PARAM_FILE_PATH).append("=");
		url.append(filePath);

		String downloadUrl = RWT.getServiceManager().getServiceHandlerUrl(
				getDownloadServiceHandlerId())
				+ url.toString();
		if (log.isTraceEnabled())
			log.debug("URL : " + downloadUrl);

		UrlLauncher launcher = RWT.getClient().getService(UrlLauncher.class);
		launcher.openURL(downloadUrl);

		// These lines are useless in the current use case but might be
		// necessary with new browsers. Stored here for memo
		// response.setContentType("application/force-download");
		// response.setHeader("Content-Disposition", contentDisposition);
		// response.setHeader("Content-Transfer-Encoding", "binary");
		// response.setHeader("Pragma", "no-cache");
		// response.setHeader("Cache-Control", "no-cache, must-revalidate");
		return null;
	}

	protected abstract String getDownloadServiceHandlerId();
}