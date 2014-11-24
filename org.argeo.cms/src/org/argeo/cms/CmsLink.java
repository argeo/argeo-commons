package org.argeo.cms;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.jcr.Node;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.service.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;

/** A link to an internal or external location. */
public class CmsLink implements CmsUiProvider, InitializingBean,
		BundleContextAware {
	private final static Log log = LogFactory.getLog(CmsLink.class);

	private String label;
	private String custom;
	private String target;
	private String image;
	private MouseListener mouseListener;

	private int verticalAlignment = SWT.CENTER;

	// internal
	//private Boolean isUrl = false;
	private Integer imageWidth, imageHeight;

	private BundleContext bundleContext;

	public CmsLink() {
		super();
	}

	public CmsLink(String label, String target) {
		this(label, target, null);
	}

	public CmsLink(String label, String target, String custom) {
		super();
		this.label = label;
		this.target = target;
		this.custom = custom;
		afterPropertiesSet();
	}

	@Override
	public void afterPropertiesSet() {
//		if (target != null) {
//			if (target.startsWith("/")) {
//				isUrl = true;
//			} else {
//				try {
//					new URL(target);
//					isUrl = true;
//				} catch (MalformedURLException e1) {
//					isUrl = false;
//				}
//			}
//		}

		if (image != null) {
			ImageData image = loadImage();
			imageWidth = image.width;
			imageHeight = image.height;
		}
	}

	@Override
	public Control createUi(final Composite parent, Node context) {
		Composite comp = new Composite(parent, SWT.BOTTOM);
		comp.setLayout(CmsUtils.noSpaceGridLayout());

		Label link = new Label(comp, SWT.NONE);
		link.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
		GridData layoutData = new GridData(SWT.CENTER, verticalAlignment, true,
				true);
		if (image != null) {
			layoutData.heightHint = imageHeight;
			if (label == null)
				layoutData.widthHint = imageWidth;
		}

		link.setLayoutData(layoutData);
		if (custom != null) {
			comp.setData(RWT.CUSTOM_VARIANT, custom);
			link.setData(RWT.CUSTOM_VARIANT, custom);
		} else {
			comp.setData(RWT.CUSTOM_VARIANT, CmsStyles.CMS_LINK);
			link.setData(RWT.CUSTOM_VARIANT, CmsStyles.CMS_LINK);
		}

		// label
		StringBuilder labelText = new StringBuilder();
		if (target != null) {
			labelText
					.append("<a style='color:inherit;text-decoration:inherit;' href=\"");
//			if (!isUrl)
//				labelText.append('#');
			labelText.append(target);
			labelText.append("\">");
		}
		if (image != null) {
			registerImageIfNeeded();
			String imageLocation = RWT.getResourceManager().getLocation(image);
			labelText.append("<img width='").append(imageWidth)
					.append("' height='").append(imageHeight)
					.append("' src=\"").append(imageLocation).append("\"/>");

			// final Image img = loadImage(parent.getDisplay());
			// link.setImage(img);
			// link.addDisposeListener(new DListener(img));
		}

		if (label != null) {
			// link.setText(label);
			labelText.append(' ').append(label);
		}

		if (target != null)
			labelText.append("</a>");

		link.setText(labelText.toString());

		// link.setCursor(link.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		// CmsSession cmsSession = (CmsSession) parent.getDisplay().getData(
		// CmsSession.KEY);
		if (mouseListener != null)
			link.addMouseListener(mouseListener);

		return comp;
	}

	private void registerImageIfNeeded() {
		ResourceManager resourceManager = RWT.getResourceManager();
		if (!resourceManager.isRegistered(image)) {
			URL res = getImageUrl();
			InputStream inputStream = null;
			try {
				IOUtils.closeQuietly(inputStream);
				inputStream = res.openStream();
				resourceManager.register(image, inputStream);
				if (log.isTraceEnabled())
					log.trace("Registered image " + image);
			} catch (Exception e) {
				throw new CmsException("Cannot load image " + image, e);
			} finally {
				IOUtils.closeQuietly(inputStream);
			}
		}
	}

	private ImageData loadImage() {
		URL url = getImageUrl();
		ImageData result = null;
		InputStream inputStream = null;
		try {
			inputStream = url.openStream();
			result = new ImageData(inputStream);
			if (log.isTraceEnabled())
				log.trace("Loaded image " + image);
		} catch (Exception e) {
			throw new CmsException("Cannot load image " + image, e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
		return result;
	}

	private URL getImageUrl() {
		URL url;
		try {
			// pure URL
			url = new URL(image);
		} catch (MalformedURLException e1) {
			// in OSGi bundle
			if (bundleContext == null)
				throw new CmsException("No bundle context available");
			url = bundleContext.getBundle().getResource(image);
		}

		if (url == null)
			throw new CmsException("No image " + image + " available.");

		return url;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setCustom(String custom) {
		this.custom = custom;
	}

	public void setTarget(String target) {
		this.target = target;
		// try {
		// new URL(target);
		// isUrl = true;
		// } catch (MalformedURLException e1) {
		// isUrl = false;
		// }
	}

	public void setImage(String image) {
		this.image = image;
	}

	@Override
	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void setMouseListener(MouseListener mouseListener) {
		this.mouseListener = mouseListener;
	}

	public void setvAlign(String vAlign) {
		if ("bottom".equals(vAlign)) {
			verticalAlignment = SWT.BOTTOM;
		} else if ("top".equals(vAlign)) {
			verticalAlignment = SWT.TOP;
		} else if ("center".equals(vAlign)) {
			verticalAlignment = SWT.CENTER;
		} else {
			throw new CmsException("Unsupported vertical allignment " + vAlign
					+ " (must be: top, bottom or center)");
		}
	}

	// private class MListener extends MouseAdapter {
	// private static final long serialVersionUID = 3634864186295639792L;
	//
	// @Override
	// public void mouseDown(MouseEvent e) {
	// if (e.button == 1) {
	// }
	// }
	// }
	//
	// private class DListener implements DisposeListener {
	// private static final long serialVersionUID = -3808587499269394812L;
	// private final Image img;
	//
	// public DListener(Image img) {
	// super();
	// this.img = img;
	// }
	//
	// @Override
	// public void widgetDisposed(DisposeEvent event) {
	// img.dispose();
	// }
	//
	// }
}
