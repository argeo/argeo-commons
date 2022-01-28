package org.argeo.cms.swt.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.argeo.cms.osgi.BundleCmsTheme;
import org.argeo.cms.swt.CmsSwtTheme;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

/** Centralises some generic {@link CmsSwtTheme} patterns. */
public class BundleCmsSwtTheme extends BundleCmsTheme implements CmsSwtTheme {
	private Map<String, ImageData> imageCache = new HashMap<>();

	private Map<String, Map<Integer, String>> iconPaths = new HashMap<>();

	public Image getImage(String path) {
		if (!imageCache.containsKey(path)) {
			try (InputStream in = getResourceAsStream(path)) {
				if (in == null)
					return null;
				ImageData imageData = new ImageData(in);
				imageCache.put(path, imageData);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		ImageData imageData = imageCache.get(path);
		Image image = new Image(Display.getCurrent(), imageData);
		return image;
	}

	/**
	 * And icon with this file name (without the extension), with a best effort to
	 * find the appropriate size, or <code>null</code> if not found.
	 * 
	 * @param name          An icon file name without path and extension.
	 * @param preferredSize the preferred size, if <code>null</code>,
	 *                      {@link #getDefaultIconSize()} will be tried.
	 */
	public Image getIcon(String name, Integer preferredSize) {
		if (preferredSize == null)
			preferredSize = getDefaultIconSize();
		Map<Integer, String> subCache;
		if (!iconPaths.containsKey(name))
			subCache = new HashMap<>();
		else
			subCache = iconPaths.get(name);
		Image image = null;
		if (!subCache.containsKey(preferredSize)) {
			Image bestMatchSoFar = null;
			paths: for (String p : getImagesPaths()) {
				int lastSlash = p.lastIndexOf('/');
				String fileName = p;
				if (lastSlash >= 0)
					fileName = p.substring(lastSlash + 1);
				int lastDot = fileName.lastIndexOf('.');
				if (lastDot >= 0)
					fileName = fileName.substring(0, lastDot);
				if (fileName.equals(name)) {// matched
					Image img = getImage(p);
					int width = img.getBounds().width;
					if (width == preferredSize) {// perfect match
						subCache.put(preferredSize, p);
						image = img;
						break paths;
					}
					if (bestMatchSoFar == null) {
						bestMatchSoFar = img;
					} else {
						if (Math.abs(width - preferredSize) < Math
								.abs(bestMatchSoFar.getBounds().width - preferredSize))
							bestMatchSoFar = img;
					}
				}
			}

			if (image == null)
				image = bestMatchSoFar;
		} else {
			image = getImage(subCache.get(preferredSize));
		}

		if (image != null && !iconPaths.containsKey(name))
			iconPaths.put(name, subCache);

		return image;
	}

}
