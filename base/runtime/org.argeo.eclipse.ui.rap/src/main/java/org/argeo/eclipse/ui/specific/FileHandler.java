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
	public final static String FORCED_DOWNLOAD_URL_BASE_PROPERTY = "argeo.rap.specific.forcedDownloadUrlBase";
	public final static String DOWNLOAD_SERVICE_NAME = "download";
	private final static Log log = LogFactory.getLog(FileHandler.class);

	public FileHandler(FileProvider provider) {
		ServiceHandler handler = new DownloadServiceHandler(provider);
		RWT.getServiceManager().registerServiceHandler(DOWNLOAD_SERVICE_NAME,
				handler);
	}

	public void openFile(String fileName, String fileId) {

		// LEGACY
		// See RAP FAQ:
		// http://wiki.eclipse.org/RAP/FAQ#How_to_provide_download_link.3F
		// And forum discussion :
		// http://www.eclipse.org/forums/index.php?t=msg&th=205487&start=0&S=43d85dacc88b505402420592109c7240

		try {
			String fullDownloadUrl = createFullDownloadUrl(fileName, fileId);
			if (log.isTraceEnabled())
				log.trace("URL : " + fullDownloadUrl);
			// URL url = new URL(fullDownloadUrl);
			UrlLauncher launcher = RWT.getClient()
					.getService(UrlLauncher.class);
			launcher.openURL(fullDownloadUrl);
			// PlatformUI.getWorkbench().getBrowserSupport()
			// .createBrowser("DownloadDialog").openURL(url);
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

	private String createFullDownloadUrl(String fileName, String fileId) {
		StringBuilder url = new StringBuilder();
		// in case RAP is proxied we need to specify the actual base URL
		// TODO find a cleaner way
		// String forcedDownloadUrlBase = System
		// .getProperty(FORCED_DOWNLOAD_URL_BASE_PROPERTY);
		// if (forcedDownloadUrlBase != null)
		// url.append(forcedDownloadUrlBase);
		// else
		// url.append(RWT.getRequest().getRequestURL());

		// TODO check how to get that cleanly when coming back online
		url.append("http://localhost:7070");
		url.append(RWT.getServiceManager().getServiceHandlerUrl(
				DOWNLOAD_SERVICE_NAME));

		url.append(createParamUrl(fileName, fileId));
		return url.toString();
	}

	private String createParamUrl(String filename, String fileId) {

		StringBuilder url = new StringBuilder();
		// url.append("?");
		// url.append(ServiceHandler.REQUEST_PARAM);
		// url.append("=downloadServiceHandler");
		url.append("&filename=");
		url.append(filename);
		url.append("&fileid=");
		url.append(fileId);
		return url.toString();
	}
}
