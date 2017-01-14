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
package org.argeo.cms.ui.workbench.jcr;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;

import org.argeo.cms.CmsException;
import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.fs.SimpleFsBrowser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * Browse the node file system.
 */
public class NodeFsBrowserView extends ViewPart {
	public final static String ID = WorkbenchUiPlugin.PLUGIN_ID + ".nodeFsBrowserView";

	private FileSystemProvider nodeFileSystemProvider;

	@Override
	public void createPartControl(Composite parent) {
		try {
			URI uri = new URI("node:///home");
			FileSystem fileSystem = nodeFileSystemProvider.getFileSystem(uri);
			if(fileSystem==null)
				fileSystem = nodeFileSystemProvider.newFileSystem(uri,null);
			Path nodePath = fileSystem.getPath("/home");
//			Path nodePath = Paths.get(uri);
			
			Path path = Paths.get("/");
			SimpleFsBrowser browser = new SimpleFsBrowser(parent, SWT.NO_FOCUS);
			browser.setInput(path, nodePath);
			browser.setLayoutData(EclipseUiUtils.fillAll());
		} catch (Exception e) {
			throw new CmsException("Cannot open file system browser", e);
		}
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	public void setNodeFileSystemProvider(FileSystemProvider nodeFileSystemProvider) {
		this.nodeFileSystemProvider = nodeFileSystemProvider;
	}
}
