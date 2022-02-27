package org.argeo.minidesktop;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/** Icons. */
public class MiniDesktopImages {

	public final Image homeIcon;
	public final Image exitIcon;
	
	public final Image terminalIcon;
	public final Image browserIcon;
	public final Image explorerIcon;
	public final Image textEditorIcon;

	public final Image folderIcon;
	public final Image fileIcon;

	public MiniDesktopImages(Display display) {
		homeIcon = loadImage(display, "nav_home@2x.png");
		exitIcon = loadImage(display, "delete@2x.png");

		terminalIcon = loadImage(display, "console_view@2x.png");
		browserIcon = loadImage(display, "external_browser@2x.png");
		explorerIcon = loadImage(display, "fldr_obj@2x.png");
		textEditorIcon = loadImage(display, "cheatsheet_obj@2x.png");

		folderIcon = loadImage(display, "fldr_obj@2x.png");
		fileIcon = loadImage(display, "file_obj@2x.png");
	}

	static Image loadImage(Display display, String path) {
		InputStream stream = MiniDesktopImages.class.getResourceAsStream(path);
		if (stream == null)
			throw new IllegalArgumentException("Image " + path + " not found");
		Image image = null;
		try {
			image = new Image(display, stream);
		} catch (SWTException ex) {
		} finally {
			try {
				stream.close();
			} catch (IOException ex) {
			}
		}
		return image;
	}
}
