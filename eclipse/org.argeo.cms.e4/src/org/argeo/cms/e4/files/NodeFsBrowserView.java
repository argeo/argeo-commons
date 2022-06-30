package org.argeo.cms.e4.files;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.argeo.cms.jcr.CmsJcrUtils;
import org.argeo.cms.swt.CmsException;
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
