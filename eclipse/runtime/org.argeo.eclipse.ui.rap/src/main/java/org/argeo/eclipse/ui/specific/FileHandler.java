package org.argeo.eclipse.ui.specific;

import java.io.InputStream;
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

	public void openFile(String fileName, String fileId, InputStream is) {
		try {
			if (log.isDebugEnabled())
				log.debug("URL : " + createFullDownloadUrl(fileName, fileId));

			URL url = new URL(createFullDownloadUrl(fileName, fileId));
			PlatformUI.getWorkbench().getBrowserSupport()
					.createBrowser("DownloadDialog").openURL(url);

			/*
			 * // New try : must define a service handler for the current file,
			 * // register it and redirect the URL to it.
			 * 
			 * // workaround : create a tmp file. String prefix = "", suffix =
			 * ""; if (fileName != null) { int ind = fileName.lastIndexOf('.');
			 * if (ind > 0) { prefix = fileName.substring(0, ind); suffix =
			 * fileName.substring(ind); } }
			 * 
			 * File tmpFile = createTmpFile(prefix, suffix, is);
			 * HttpServletResponse response = RWT.getResponse();
			 * 
			 * DownloadHandler dh = new DownloadHandler(tmpFile.getName(),
			 * tmpFile, "application/pdf", fileName);
			 * 
			 * log.debug("Got a DH : " + dh.toString());
			 * 
			 * // TODO : should try with that. // String encodedURL =
			 * RWT.getResponse().encodeURL(url.toString());
			 * response.sendRedirect(dh.getURL());
			 */

			// final Browser browser = new Browser(parent, SWT.NONE);
			// browser.setText(createDownloadHtml("test.pdf", "Download file"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		// // Which file to download?
		// String fileName = RWT.getRequest().getParameter( "filename" );
		// // Get the file content
		// byte[] download = MyDataStore.getByteArrayData( fileName );
		// // Send the file in the response
		// HttpServletResponse response = RWT.getResponse();
		// response.setContentType( "application/octet-stream" );
		// response.setContentLength( download.length );
		// String contentDisposition = "attachment; filename=\"" + fileName +
		// "\"";
		// response.setHeader( "Content-Disposition", contentDisposition );
		// try {
		// response.getOutputStream().write( download );
		// } catch( IOException e1 ) {
		// e1.printStackTrace();
		// }
		//
		//

		/**
		 * try {
		 * 
		 * // / workaround : create a tmp file. String prefix = "", suffix = "";
		 * if (fileName != null) { int ind = fileName.lastIndexOf('.'); if (ind
		 * > 0) { prefix = fileName.substring(0, ind); suffix =
		 * fileName.substring(ind); } }
		 * 
		 * File tmpFile = createTmpFile(prefix, suffix, is);
		 * 
		 * // Send the file in the response HttpServletResponse response =
		 * RWT.getResponse(); byte[] ba = null; ba =
		 * FileUtils.readFileToByteArray(tmpFile);
		 * 
		 * long l = tmpFile.length();
		 * 
		 * if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) throw new
		 * ArgeoException("IllegalArgumentException : " + l +
		 * " cannot be cast to int without changing its value.");
		 * response.setContentLength((int) l);
		 * 
		 * // response.setContentLength(ba.length);
		 * 
		 * // String contentDisposition = "attachment; filename=\"" + fileName
		 * // + "\""; String contentDisposition = "attachment; filename=\"" +
		 * fileName + "\"";
		 * response.setContentType("application/force-download");
		 * response.setHeader("Content-Disposition", contentDisposition);
		 * response.setHeader("Content-Transfer-Encoding", "binary");
		 * response.setHeader("Pragma", "no-cache");
		 * response.setHeader("Cache-Control", "no-cache, must-revalidate");
		 * 
		 * // must-revalidate");
		 * 
		 * if (log.isDebugEnabled()) { log.debug("Header Set "); }
		 * 
		 * // header("Content-Type: application/force-download; name=\"".
		 * $localName."\""); // 852 header("Content-Transfer-Encoding: binary");
		 * // 853 if($gzip){ // 854 header("Content-Encoding: gzip"); // 855 //
		 * If gzip, recompute data size! // 856 $gzippedData = //
		 * ($data?gzencode
		 * ($filePathOrData,9):gzencode(file_get_contents($filePathOrData), //
		 * 9)); // 857 $size = strlen($gzippedData); // 858 } // 859
		 * header("Content-Length: ".$size); // 860 if ($isFile && ($size != 0))
		 * header("Content-Range: bytes 0-" // . ($size - 1) . "/" . $size .
		 * ";"); // 861 //
		 * header("Content-Disposition: attachment; filename=\"".
		 * $localName."\""); // 862 header("Expires: 0"); // 863
		 * header("Cache-Control: no-cache, must-revalidate"); // 864
		 * header("Pragma: no-cache");
		 * 
		 * // IOUtils.copy(is, response.getOutputStream());
		 * response.getOutputStream().write(ba); //
		 * Error.show("In Open File for RAP.");
		 * 
		 * 
		 * 
		 * 
		 * } catch (IOException ioe) {
		 * 
		 * throw new ArgeoException("Cannot copy input stream from file " +
		 * fileName + " to HttpServletResponse", ioe); }
		 */

	}

	// private File createTmpFile(String prefix, String suffix, InputStream is)
	// {
	// File tmpFile = null;
	// OutputStream os = null;
	// try {
	// tmpFile = File.createTempFile(prefix, suffix);
	// os = new FileOutputStream(tmpFile);
	// IOUtils.copy(is, os);
	// } catch (IOException e) {
	// throw new ArgeoException("Cannot open file " + prefix + "."
	// + suffix, e);
	// } finally {
	// IOUtils.closeQuietly(os);
	// }
	// return tmpFile;
	// }

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
