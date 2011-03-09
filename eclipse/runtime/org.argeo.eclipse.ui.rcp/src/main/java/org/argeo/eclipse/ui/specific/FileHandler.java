package org.argeo.eclipse.ui.specific;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.argeo.ArgeoException;

/**
 * Abstraction that enable to implement runtime environment specific (typically
 * RCP or RAP) methods while dealing with files in the UI.
 * 
 */
public class FileHandler {

	// unused file provider : collateral effects of single sourcing, this File
	// provider is compulsory for RAP file handler
	public FileHandler(FileProvider jfp) {
	}

	public void openFile(String fileName, String fileId, InputStream is) {

		String prefix = "", extension = "";
		if (fileName != null) {
			int ind = fileName.lastIndexOf('.');
			if (ind > 0) {
				prefix = fileName.substring(0, ind);
				extension = fileName.substring(ind);
			}
		}

		File file = createTmpFile(prefix, extension, is);

		try {
			Desktop desktop = null;
			if (Desktop.isDesktopSupported()) {
				desktop = Desktop.getDesktop();
			}
			desktop.open(file);
		} catch (IOException e) {
			throw new ArgeoException("Cannot open file " + file.getName(), e);
		}
	}

	private void openFile(File file) {
		try {
			Desktop desktop = null;
			if (Desktop.isDesktopSupported()) {
				desktop = Desktop.getDesktop();
			}
			desktop.open(file);
		} catch (IOException e) {
			throw new ArgeoException("Cannot open file " + file.getName(), e);
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
