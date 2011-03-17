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
 * @author bsinou
 * 
 */
public class FileHandler {

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
