package org.argeo.cms.ui.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.ux.CmsStyle;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.jcr.CmsJcrUtils;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ui.CmsUiProvider;
import org.argeo.jcr.JcrException;
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
	private final static CmsLog log = CmsLog.getLog(CmsLink.class);
	private BundleContext bundleContext;

	private String label;
	private String style;
	private String target;
	private String image;
	private boolean openNew = false;
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
		this(label, target, (String) null);
	}

	public CmsLink(String label, String target, CmsStyle style) {
		this(label, target, style != null ? style.style() : null);
	}

	public CmsLink(String label, String target, String style) {
		super();
		this.label = label;
		this.target = target;
		this.style = style;
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
		comp.setLayout(CmsSwtUtils.noSpaceGridLayout());

		Label link = new Label(comp, SWT.NONE);
		CmsSwtUtils.markup(link);
		GridData layoutData = new GridData(horizontalAlignment, verticalAlignment, false, false);
		if (image != null) {
			if (imageHeight != null)
				layoutData.heightHint = imageHeight;
			if (label == null)
				if (imageWidth != null)
					layoutData.widthHint = imageWidth;
		}

		link.setLayoutData(layoutData);
		CmsSwtUtils.style(comp, style != null ? style : getDefaultStyle());
		CmsSwtUtils.style(link, style != null ? style : getDefaultStyle());

		// label
		StringBuilder labelText = new StringBuilder();
		if (loggedInTarget != null && isLoggedIn()) {
			labelText.append("<a style='color:inherit;text-decoration:inherit;' href=\"");
			if (loggedInTarget.equals("")) {
				try {
					Node homeNode = CmsJcrUtils.getUserHome(context.getSession());
					String homePath = homeNode.getPath();
					labelText.append("/#" + homePath);
				} catch (RepositoryException e) {
					throw new JcrException("Cannot get home path", e);
				}
			} else {
				labelText.append(loggedInTarget);
			}
			labelText.append("\">");
		} else if (target != null) {
			labelText.append("<a style='color:inherit;text-decoration:inherit;' href='");
			labelText.append(target).append("'");
			if (openNew) {
				labelText.append(" target='_blank'");
			}
			labelText.append(">");
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
			try (InputStream inputStream = res.openStream()) {
				resourceManager.register(image, inputStream);
				if (log.isTraceEnabled())
					log.trace("Registered image " + image);
			} catch (IOException e) {
				throw new RuntimeException("Cannot load image " + image, e);
			}
		}
	}

	private ImageData loadImage() {
		URL url = getImageUrl();
		ImageData result = null;
		try (InputStream inputStream = url.openStream()) {
			result = new ImageData(inputStream);
			if (log.isTraceEnabled())
				log.trace("Loaded image " + image);
		} catch (IOException e) {
			throw new RuntimeException("Cannot load image " + image, e);
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
			throw new IllegalStateException("No image " + image + " available.");

		return url;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	/** @deprecated Use {@link #setStyle(String)} instead. */
	@Deprecated
	public void setCustom(String custom) {
		this.style = custom;
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
			throw new IllegalArgumentException(
					"Unsupported vertical alignment " + vAlign + " (must be: top, bottom or center)");
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

	public void setOpenNew(boolean openNew) {
		this.openNew = openNew;
	}

	protected String getDefaultStyle() {
		return SimpleStyle.link.name();
	}
}
