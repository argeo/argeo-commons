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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import org.argeo.ArgeoException;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * RCP specific command handler to open a file.
 * 
 * The parameter "URI" is used to determine the correct method to open it.
 * 
 * Various instances of this handler with different command ID might coexist in
 * order to provide context specific open file service.
 * 
 */
public class OpenFile extends AbstractHandler {
	// private final static Log log = LogFactory.getLog(OpenFile.class);

	public final static String PARAM_FILE_NAME = "param.fileName";
	public final static String PARAM_FILE_URI = "param.fileURI";

	private final static String FILE_SCHEME = "file";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		@SuppressWarnings("unused")
		@Deprecated
		String fileName = event.getParameter(PARAM_FILE_NAME);
		String fileUri = event.getParameter(PARAM_FILE_URI);

		// sanity check
		if (fileUri == null || "".equals(fileUri.trim()))
			return null;

		Desktop desktop = null;
		if (Desktop.isDesktopSupported()) {
			desktop = Desktop.getDesktop();
		}

		File file = getFileFromUri(fileUri);
		if (file != null)
			try {
				desktop.open(file);
			} catch (IOException e) {
				throw new ArgeoException("Unable to open file with URI: "
						+ fileUri, e);
			}

		return null;
	}

	protected File getFileFromUri(String uri) {
		if (uri.startsWith(FILE_SCHEME)) {
			String path = uri.substring((FILE_SCHEME + "://").length());
			return new File(path);
		}
		return null;
	}
}