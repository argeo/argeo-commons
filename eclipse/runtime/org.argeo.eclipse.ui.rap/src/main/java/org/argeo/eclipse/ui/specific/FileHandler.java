package org.argeo.eclipse.ui.specific;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.eclipse.rwt.RWT;

public class FileHandler {

	private static Log log = LogFactory.getLog(FileHandler.class);

	public FileHandler() {
	}

	public void openFile(String fileName, InputStream is) {

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

		try {

			// / workaround : create a tmp file.
			String prefix = "", suffix = "";
			if (fileName != null) {
				int ind = fileName.lastIndexOf('.');
				if (ind > 0) {
					prefix = fileName.substring(0, ind);
					suffix = fileName.substring(ind);
				}
			}

			File tmpFile = createTmpFile(prefix, suffix, is);

			// Send the file in the response
			HttpServletResponse response = RWT.getResponse();
			byte[] ba = null;
			ba = FileUtils.readFileToByteArray(tmpFile);

			response.setContentLength(ba.length);

			// String contentDisposition = "attachment; filename=\"" + fileName
			// + "\"";
			String contentDisposition = "attachment; filename=\"" + fileName
					+ "\"";
			response.setContentType("application/force-download");
			response.setHeader("Content-Disposition", contentDisposition);
			response.setHeader("Content-Transfer-Encoding", "binary");
			response.setHeader("Pragma", "no-cache");
			response.setHeader("Cache-Control", "no-cache, must-revalidate");

			// must-revalidate");

			if (log.isDebugEnabled()) {
				log.debug("Header Set ");
			}

			// header("Content-Type: application/force-download; name=\"".$localName."\"");
			// 852 header("Content-Transfer-Encoding: binary");
			// 853 if($gzip){
			// 854 header("Content-Encoding: gzip");
			// 855 // If gzip, recompute data size!
			// 856 $gzippedData =
			// ($data?gzencode($filePathOrData,9):gzencode(file_get_contents($filePathOrData),
			// 9));
			// 857 $size = strlen($gzippedData);
			// 858 }
			// 859 header("Content-Length: ".$size);
			// 860 if ($isFile && ($size != 0)) header("Content-Range: bytes 0-"
			// . ($size - 1) . "/" . $size . ";");
			// 861
			// header("Content-Disposition: attachment; filename=\"".$localName."\"");
			// 862 header("Expires: 0");
			// 863 header("Cache-Control: no-cache, must-revalidate");
			// 864 header("Pragma: no-cache");

			// IOUtils.copy(is, response.getOutputStream());
			response.getOutputStream().write(ba);
			// Error.show("In Open File for RAP.");
		} catch (IOException ioe) {

			throw new ArgeoException("Cannot copy input stream from file "
					+ fileName + " to HttpServletResponse", ioe);
		}

	}

	private File createTmpFile(String prefix, String suffix, InputStream is) {
		File tmpFile = null;
		OutputStream os = null;
		try {
			tmpFile = File.createTempFile(prefix, suffix);
			os = new FileOutputStream(tmpFile);
			IOUtils.copy(is, os);
		} catch (IOException e) {
			throw new ArgeoException("Cannot open file " + prefix + "."
					+ suffix, e);
		} finally {
			IOUtils.closeQuietly(os);
		}
		return tmpFile;
	}

}
