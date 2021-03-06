package org.argeo.cms.ui.util;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeUtils;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.ui.CmsStyles;
import org.argeo.cms.ui.CmsUiProvider;
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

/** A link to an internal or external location. */
public class CmsLink implements CmsUiProvider {
	private final static Log log = LogFactory.getLog(CmsLink.class);
	private BundleContext bundleContext;

	private String label;
	private String custom;
	private String target;
	private String image;
	private MouseListener mouseListener;

	private int horizontalAlignment = SWT.CENTER;
	private int verticalAlignment = SWT.CENTER;

	private String loggedInLabel = null;
	private String loggedInTarget = null;

	// internal
	// private Boolean isUrl = false;
	private Integer imageWidth, imageHeight;

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
		init();
	}

	public void init() {
		if (image != null) {
			ImageData image = loadImage();
			if (imageHeight == null && imageWidth == null) {
				imageWidth = image.width;
				imageHeight = image.height;
			} else if (imageHeight == null) {
				imageHeight = (imageWidth * image.height) / image.width;
			} else if (imageWidth == null) {
				imageWidth = (imageHeight * image.width) / image.height;
			}
		}
	}

	/** @return {@link Composite} with a single {@link Label} child. */
	@Override
	public Control createUi(final Composite parent, Node context) {
//		if (image != null && (imageWidth == null || imageHeight == null)) {
//			throw new CmsException("Image is not properly configured."
//					+ " Make sure bundleContext property is set and init() method has been called.");
//		}

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(CmsUiUtils.noSpaceGridLayout());

		Label link = new Label(comp, SWT.NONE);
		link.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
		GridData layoutData = new GridData(horizontalAlignment, verticalAlignment, false, false);
		if (image != null) {
			if (imageHeight != null)
				layoutData.heightHint = imageHeight;
			if (label == null)
				if (imageWidth != null)
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
		if (loggedInTarget != null && isLoggedIn()) {
			labelText.append("<a style='color:inherit;text-decoration:inherit;' href=\"");
			if (loggedInTarget.equals("")) {
				try {
					Node homeNode = NodeUtils.getUserHome(context.getSession());
					String homePath = homeNode.getPath();
					labelText.append("/#" + homePath);
				} catch (RepositoryException e) {
					throw new CmsException("Cannot get home path", e);
				}
			} else {
				labelText.append(loggedInTarget);
			}
			labelText.append("\">");
		} else if (target != null) {
			labelText.append("<a style='color:inherit;text-decoration:inherit;' href=\"");
			labelText.append(target);
			labelText.append("\">");
		}
		if (image != null) {
			registerImageIfNeeded();
			String imageLocation = RWT.getResourceManager().getLocation(image);
			labelText.append("<img");
			if (imageWidth != null)
				labelText.append(" width='").append(imageWidth).append('\'');
			if (imageHeight != null)
				labelText.append(" height='").append(imageHeight).append('\'');
			labelText.append(" src=\"").append(imageLocation).append("\"/>");

		}

		if (loggedInLabel != null && isLoggedIn()) {
			labelText.append(' ').append(loggedInLabel);
		} else if (label != null) {
			labelText.append(' ').append(label);
		}

		if ((loggedInTarget != null && isLoggedIn()) || target != null)
			labelText.append("</a>");

		link.setText(labelText.toString());

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
			url = bundleContext.getBundle().getResource(image);
		}

		if (url == null)
			throw new CmsException("No image " + image + " available.");

		return url;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
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

	public void setLoggedInLabel(String loggedInLabel) {
		this.loggedInLabel = loggedInLabel;
	}

	public void setLoggedInTarget(String loggedInTarget) {
		this.loggedInTarget = loggedInTarget;
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
			throw new CmsException("Unsupported vertical allignment " + vAlign + " (must be: top, bottom or center)");
		}
	}

	protected boolean isLoggedIn() {
		return !CurrentUser.isAnonymous();
	}

	public void setImageWidth(Integer imageWidth) {
		this.imageWidth = imageWidth;
	}

	public void setImageHeight(Integer imageHeight) {
		this.imageHeight = imageHeight;
	}

}
