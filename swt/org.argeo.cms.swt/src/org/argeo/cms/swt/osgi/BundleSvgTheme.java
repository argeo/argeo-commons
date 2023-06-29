package org.argeo.cms.swt.osgi;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;

/** Theme which can dynamically create icons from SVG data. */
public class BundleSvgTheme extends BundleCmsSwtTheme {
	private final static Logger logger = System.getLogger(BundleSvgTheme.class.getName());

	private Map<String, Map<Integer, ImageData>> imageDataCache = Collections.synchronizedMap(new HashMap<>());

	private Map<Integer, ImageTranscoder> transcoders = Collections.synchronizedMap(new HashMap<>());

	private final static String IMAGE_CACHE_KEY = BundleSvgTheme.class.getName() + ".imageCache";

	@Override
	public Image getIcon(String name, Integer preferredSize) {
		String path = "icons/types/svg/" + name + ".svg";
		return createImageFromSvg(path, preferredSize);
	}

	@SuppressWarnings("unchecked")
	protected Image createImageFromSvg(String path, Integer preferredSize) {
		Display display = Display.getCurrent();
		Objects.requireNonNull(display, "Not a user interface thread");

		Map<String, Map<Integer, Image>> imageCache = (Map<String, Map<Integer, Image>>) display
				.getData(IMAGE_CACHE_KEY);
		if (imageCache == null)
			display.setData(IMAGE_CACHE_KEY, new HashMap<String, Map<Integer, Image>>());
		imageCache = (Map<String, Map<Integer, Image>>) display.getData(IMAGE_CACHE_KEY);

		Image image = null;
		if (imageCache.containsKey(path)) {
			image = imageCache.get(path).get(preferredSize);
		}
		if (image != null)
			return image;
		ImageData imageData = loadFromSvg(path, preferredSize);
		image = new Image(display, imageData);
		if (!imageCache.containsKey(path))
			imageCache.put(path, Collections.synchronizedMap(new HashMap<>()));
		imageCache.get(path).put(preferredSize, image);
		return image;
	}

	protected ImageData loadFromSvg(String path, int size) {
		ImageData imageData = null;
		if (imageDataCache.containsKey(path))
			imageData = imageDataCache.get(path).get(size);
		if (imageData != null)
			return imageData;

		ImageTranscoder transcoder = null;
		synchronized (this) {
			transcoder = transcoders.get(size);
			if (transcoder == null) {
				transcoder = new PNGTranscoder();
				transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) size);
				transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) size);
				transcoder.addTranscodingHint(PNGTranscoder.KEY_BACKGROUND_COLOR, new Color(255, 255, 255, 0));
				transcoders.put(size, transcoder);
			}
		}
		try (InputStream in = getResourceAsStream(path); ByteArrayOutputStream out = new ByteArrayOutputStream();) {
			if (in == null)
				throw new IllegalArgumentException(path + " not found");
			TranscoderInput input = new TranscoderInput(in);
			TranscoderOutput output = new TranscoderOutput(out);
			transcoder.transcode(input, output);
			try (InputStream imageIn = new ByteArrayInputStream(out.toByteArray())) {
				imageData = new ImageData(imageIn);
			}
			logger.log(Level.DEBUG, () -> "Generated " + size + "x" + size + " PNG icon from " + path);
		} catch (IOException | TranscoderException e) {
			throw new RuntimeException("Cannot transcode SVG " + path, e);
		}

		// cache it
		if (!imageDataCache.containsKey(path))
			imageDataCache.put(path, Collections.synchronizedMap(new HashMap<>()));
		imageDataCache.get(path).put(size, imageData);

		return imageData;
	}

	@Override
	public void init(BundleContext bundleContext, Map<String, String> properties) {
		super.init(bundleContext, properties);

		// preload all icons
//		paths: for (String p : getImagesPaths()) {
//			if (!p.endsWith(".svg"))
//				continue paths;
//			createImageFromSvg(p, getDefaultIconSize());
//		}
	}

//	@Override
//	public void destroy(BundleContext bundleContext, Map<String, String> properties) {
//		Display display = Display.getDefault();
//		if (display != null)
//			for (String path : imageCache.keySet()) {
//				for (Image image : imageCache.get(path).values()) {
//					display.syncExec(() -> image.dispose());
//				}
//			}
//		super.destroy(bundleContext, properties);
//	}

}
