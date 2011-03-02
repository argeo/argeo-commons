package org.argeo.eclipse.ui.specific;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.argeo.ArgeoException;

//import org.apache.commons.io;

public class FileHandler {

	private BufferedInputStream bis;

	public FileHandler() {
	}

	public File createTmpFile(String fileName, String suffix, InputStream is) {
		File tmpFile = null;
		OutputStream os = null;
		try {
			tmpFile = File.createTempFile(fileName, suffix);
			os = new FileOutputStream(tmpFile);
			IOUtils.copy(is, os);
		} catch (IOException e) {
			throw new ArgeoException("Cannot open file " + fileName, e);
		} finally {
			IOUtils.closeQuietly(os);
		}
		return tmpFile;
	}

	public void openFile(String fileName, InputStream is) {
	
		String prefix ="", extension = "";
		
		if (fileName != null){
			int ind = fileName.
			if (true){
				}
			}
			
		prefix = .substring(0,
				node.getName().lastIndexOf('.'));
		extension = node.getName().substring(
				node.getName().lastIndexOf('.'));
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
	
	
	public void openFile(File file) {
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
}
