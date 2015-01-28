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
 * Rap specific command handler to open a file retrieved from the server. It
 * forwards the request to the correct service after encoding file name and path
 * in the request URI.
 * 
 * The parameter "URI" is used to determine the correct file service, the path
 * and the file name. An optional file name can be precised to present a
 * different file name as the one used to retrieve it to the end user/
 * 
 * Various instances of this handler with different command ID might coexist in
 * order to provide context specific download service.
 * 
 * The instance specific service is called by its ID and must have been
 * externally created
 */
public class OpenFile extends AbstractHandler {
	private final static Log log = LogFactory.getLog(OpenFile.class);

	/* DEPENDENCY INJECTION */
	private String openFileServiceId;

	public final static String PARAM_FILE_NAME = OpenFileService.PARAM_FILE_NAME;
	public final static String PARAM_FILE_URI = OpenFileService.PARAM_FILE_URI; // "param.fileURI";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String fileName = event.getParameter(PARAM_FILE_NAME);
		String fileUri = event.getParameter(PARAM_FILE_URI);

		// sanity check
		if (fileUri == null || "".equals(fileUri.trim())
				|| openFileServiceId == null
				|| "".equals(openFileServiceId.trim()))
			return null;

		StringBuilder url = new StringBuilder();
		url.append(RWT.getServiceManager().getServiceHandlerUrl(
				openFileServiceId));

		url.append("&").append(PARAM_FILE_NAME).append("=");
		url.append(fileName);
		url.append("&").append(PARAM_FILE_URI).append("=");
		url.append(fileUri);

		String downloadUrl = url.toString();
		if (log.isTraceEnabled())
			log.debug("URL : " + downloadUrl);

		UrlLauncher launcher = RWT.getClient().getService(UrlLauncher.class);
		// FIXME: find a way to manage correctly the addition of the base
		// context for the workbench.
		launcher.openURL("/ui" + downloadUrl);

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
	public void setOpenFileServiceId(String openFileServiceId) {
		this.openFileServiceId = openFileServiceId;
	}
}