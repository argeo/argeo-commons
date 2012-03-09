/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.rwt.RWT;
import org.eclipse.rwt.service.IServiceHandler;
import org.eclipse.rwt.service.IServiceManager;
import org.eclipse.ui.PlatformUI;

/**
 * RAP SPECIFIC handler to enable the opening of a download dialog box triggered
 * by whatever action in the UI
 * 
 * Manages the registration of the effective DownloadServiceHandler at
 * instantiation time.
 * 
 * Manages the process of forwarding the request to the handler at runtime to
 * open the dialog box
 * 
 */
public class FileHandler {
	public final static String FORCED_DOWNLOAD_URL_BASE_PROPERTY = "argeo.rap.specific.forcedDownloadUrlBase";

	private final static Log log = LogFactory.getLog(FileHandler.class);

	public FileHandler(FileProvider provider) {
		// Instantiate and register the DownloadServicHandler.
		IServiceManager manager = RWT.getServiceManager();
		IServiceHandler handler = new DownloadServiceHandler(provider);
		manager.registerServiceHandler("downloadServiceHandler", handler);
	}

	public void openFile(String fileName, String fileId) {

		// See RAP FAQ:
		// http://wiki.eclipse.org/RAP/FAQ#How_to_provide_download_link.3F
		// And forum discussion :
		// http://www.eclipse.org/forums/index.php?t=msg&th=205487&start=0&S=43d85dacc88b505402420592109c7240

		try {
			if (log.isTraceEnabled())
				log.trace("URL : " + createFullDownloadUrl(fileName, fileId));

			URL url = new URL(createFullDownloadUrl(fileName, fileId));
			PlatformUI.getWorkbench().getBrowserSupport()
					.createBrowser("DownloadDialog").openURL(url);
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
		String forcedDownloadUrlBase = System
				.getProperty(FORCED_DOWNLOAD_URL_BASE_PROPERTY);
		if (forcedDownloadUrlBase != null)
			url.append(forcedDownloadUrlBase);
		else
			url.append(RWT.getRequest().getRequestURL());
		url.append(createParamUrl(fileName, fileId));
		return url.toString();
	}

	private String createParamUrl(String filename, String fileId) {
		StringBuilder url = new StringBuilder();
		url.append("?");
		url.append(IServiceHandler.REQUEST_PARAM);
		url.append("=downloadServiceHandler");
		url.append("&filename=");
		url.append(filename);
		url.append("&fileid=");
		url.append(fileId);
		String encodedURL = RWT.getResponse().encodeURL(url.toString());
		return encodedURL;
	}
}
