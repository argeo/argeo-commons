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
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.UrlLauncher;
import org.eclipse.rap.rwt.service.ServiceHandler;

/**
 * RAP SPECIFIC handler to enable the opening of a download dialog box triggered
 * by whatever action in the UI
 * 
 * Manages the registration of the effective DownloadServiceHandler at
 * instantiation time.
 * 
 * Manages the process of forwarding the request to the handler at runtime to
 * open the dialog box encodedURL
 */
public class FileHandler {
	public final static String DOWNLOAD_SERVICE_NAME = "argeo.rap.download.service";
	private final static Log log = LogFactory.getLog(FileHandler.class);

	public FileHandler(FileProvider provider) {
		ServiceHandler handler = new DownloadServiceHandler(provider);
		RWT.getServiceManager().registerServiceHandler(DOWNLOAD_SERVICE_NAME,
				handler);
	}

	public void openFile(String fileName, String fileId) {
		try {
			String downloadUrl = RWT.getServiceManager().getServiceHandlerUrl(
					DOWNLOAD_SERVICE_NAME)
					+ createParamUrl(fileName, fileId); 
			if (log.isTraceEnabled())
				log.debug("URL : " + downloadUrl);
			UrlLauncher launcher = RWT.getClient()
					.getService(UrlLauncher.class);
			launcher.openURL(downloadUrl);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// These lines are useless in the current use case but might be
		// necessary with new browsers. Stored here for memo
		// response.setContentType("application/force-download");
		// response.setHeader("Content-Disposition", contentDisposition);
		// response.setHeader("Content-Transfer-Encoding", "binary");
		// response.setHeader("Pragma", "no-cache");
		// response.setHeader("Cache-Control", "no-cache, must-revalidate");
	}

	private String createParamUrl(String filename, String fileId) {
		StringBuilder url = new StringBuilder();
		url.append("&filename=");
		url.append(filename);
		url.append("&fileid=");
		url.append(fileId);
		return url.toString();
	}
}