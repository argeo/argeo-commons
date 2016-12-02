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
import org.argeo.eclipse.ui.utils.SingleSourcingConstants;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.UrlLauncher;

/**
 * RWT specific object to open a file retrieved from the server. It forwards the
 * request to the correct service after encoding file name and path in the
 * request URI.
 * 
 * <p>
 * The parameter "URI" is used to determine the correct file service, the path
 * and the file name. An optional file name can be added to present the end user
 * with a different file name as the one used to retrieve it.
 * </p>
 * 
 * 
 * <p>
 * The instance specific service is called by its ID and must have been
 * externally created
 * </p>
 */
public class OpenFile extends AbstractHandler {
	private final static Log log = LogFactory.getLog(OpenFile.class);

	public final static String ID = SingleSourcingConstants.OPEN_FILE_CMD_ID;
	public final static String PARAM_FILE_NAME = SingleSourcingConstants.PARAM_FILE_NAME;
	public final static String PARAM_FILE_URI = SingleSourcingConstants.PARAM_FILE_URI;;
	/* DEPENDENCY INJECTION */
	private String openFileServiceId;

	public Object execute(ExecutionEvent event) {
		String fileName = event.getParameter(PARAM_FILE_NAME);
		String fileUri = event.getParameter(PARAM_FILE_URI);
		// Sanity check
		if (fileUri == null || "".equals(fileUri.trim()) || openFileServiceId == null
				|| "".equals(openFileServiceId.trim()))
			return null;

		org.argeo.eclipse.ui.specific.OpenFile openFileClient = new org.argeo.eclipse.ui.specific.OpenFile();
		openFileClient.execute(openFileServiceId, fileUri, fileName);
		return null;
	}

	public Object execute(String openFileServiceId, String fileUri, String fileName) {
		// // Sanity check
		// if (fileUri == null || "".equals(fileUri.trim())
		// || openFileServiceId == null
		// || "".equals(openFileServiceId.trim()))
		// return null;

		StringBuilder url = new StringBuilder();
		url.append(RWT.getServiceManager().getServiceHandlerUrl(openFileServiceId));

		url.append("&").append(SingleSourcingConstants.PARAM_FILE_NAME).append("=");
		url.append(fileName);
		url.append("&").append(SingleSourcingConstants.PARAM_FILE_URI).append("=");
		url.append(fileUri);

		String downloadUrl = url.toString();
		if (log.isTraceEnabled())
			log.debug("URL : " + downloadUrl);

		UrlLauncher launcher = RWT.getClient().getService(UrlLauncher.class);
		launcher.openURL(downloadUrl);
		return null;
	}

	/* DEPENDENCY INJECTION */
	public void setOpenFileServiceId(String openFileServiceId) {
		this.openFileServiceId = openFileServiceId;
	}
}
