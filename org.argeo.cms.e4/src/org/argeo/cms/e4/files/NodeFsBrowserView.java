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
package org.argeo.cms.e4.files;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.argeo.api.NodeUtils;
import org.argeo.cms.CmsException;
import org.argeo.eclipse.ui.fs.AdvancedFsBrowser;
import org.argeo.eclipse.ui.fs.SimpleFsBrowser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/** Browse the node file system. */
public class NodeFsBrowserView {
	// public final static String ID = WorkbenchUiPlugin.PLUGIN_ID +
	// ".nodeFsBrowserView";

	@Inject
	FileSystemProvider nodeFileSystemProvider;

	@PostConstruct
	public void createPartControl(Composite parent) {
		try {
			//URI uri = new URI("node://root:demo@localhost:7070/");
			URI uri = new URI("node:///");
			FileSystem fileSystem = nodeFileSystemProvider.getFileSystem(uri);
			if (fileSystem == null)
				fileSystem = nodeFileSystemProvider.newFileSystem(uri, null);
			Path nodePath = fileSystem.getPath("/");

			Path localPath = Paths.get(System.getProperty("user.home"));

			SimpleFsBrowser browser = new SimpleFsBrowser(parent, SWT.NO_FOCUS);
			browser.setInput(nodePath, localPath);
//			AdvancedFsBrowser browser = new AdvancedFsBrowser();
//			browser.createUi(parent, localPath);
		} catch (Exception e) {
			throw new CmsException("Cannot open file system browser", e);
		}
	}

	public void setFocus() {
	}
}
