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

	private FileProvider provider;

	public FileHandler(FileProvider provider) {
		this.provider = provider;
	}

	public void openFile(String fileName, String fileId) {
		String tmpFileName = fileName;
		String prefix = "", extension = "";
		if (fileName != null) {
			int ind = fileName.lastIndexOf('.');
			if (ind > 0) {
				prefix = fileName.substring(0, ind);
				extension = fileName.substring(ind);
			}
		}

		InputStream is = null;
		try {
			is = provider.getInputStreamFromFileId(fileId);
			File file = createTmpFile(prefix, extension, is);
			tmpFileName = file.getName();
			Desktop desktop = null;
			if (Desktop.isDesktopSupported()) {
				desktop = Desktop.getDesktop();
			}
			desktop.open(file);
		} catch (IOException e) {
			// Note : tmpFileName = fileName if the error has been thrown while
			// creating the tmpFile.
			throw new ArgeoException("Cannot open file " + tmpFileName, e);
		} finally {
			IOUtils.closeQuietly(is);
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
