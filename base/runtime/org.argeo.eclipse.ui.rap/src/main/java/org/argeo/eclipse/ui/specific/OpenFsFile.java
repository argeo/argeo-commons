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
 * Rap specific command handler to open a file stored in the server file system.
 * The file absolute path and name must be passed as parameters.
 * 
 * It relies on an existing {@link DownloadFsFileService} to forward the
 * corresponding file to the user browser.
 * 
 */
public class OpenFsFile extends AbstractHandler {
	private final static Log log = LogFactory.getLog(OpenFsFile.class);

	/* DEPENDENCY INJECTION */
	private String serviceId;

	public final static String PARAM_FILE_NAME = DownloadFsFileService.PARAM_FILE_NAME;
	public final static String PARAM_FILE_PATH = DownloadFsFileService.PARAM_FILE_PATH;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String fileName = event.getParameter(PARAM_FILE_NAME);
		String filePath = event.getParameter(PARAM_FILE_PATH);

		// sanity check
		if (serviceId == null || "".equals(serviceId.trim())
				|| fileName == null || "".equals(fileName.trim())
				|| filePath == null || "".equals(filePath.trim()))
			return null;

		StringBuilder url = new StringBuilder();
		url.append("&").append(PARAM_FILE_NAME).append("=");
		url.append(fileName);
		url.append("&").append(PARAM_FILE_PATH).append("=");
		url.append(filePath);

		String downloadUrl = RWT.getServiceManager().getServiceHandlerUrl(
				serviceId)
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

	/* DEPENDENCY INJECTION */
	public void setDownloadServiceHandlerId(String downloadServiceHandlerId) {
		this.serviceId = downloadServiceHandlerId;
	}
}