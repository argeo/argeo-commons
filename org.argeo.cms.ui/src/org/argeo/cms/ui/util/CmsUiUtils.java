package org.argeo.cms.ui.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.argeo.api.NodeConstants;
import org.argeo.api.NodeUtils;
import org.argeo.cms.ui.CmsConstants;
import org.argeo.cms.ui.CmsView;
import org.argeo.eclipse.ui.Selected;
import org.argeo.eclipse.ui.specific.EclipseUiSpecificUtils;
import org.argeo.jcr.JcrUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.service.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/** Static utilities for the CMS framework. */
public class CmsUiUtils implements CmsConstants {
	// private final static Log log = LogFactory.getLog(CmsUiUtils.class);

	/*
	 * CMS VIEW
	 */

	/** Sends an event via {@link CmsView#sendEvent(String, Map)}. */
	public static void sendEventOnSelect(Control control, String topic, Map<String, Object> properties) {
		SelectionListener listener = (Selected) (e) -> {
			CmsView.getCmsView(control.getParent()).sendEvent(topic, properties);
		};
		if (control instanceof Button) {
			((Button) control).addSelectionListener(listener);
		} else
			throw new UnsupportedOperationException("Control type " + control.getClass() + " is not supported.");
	}

	/**
	 * Convenience method to sends an event via
	 * {@link CmsView#sendEvent(String, Map)}.
	 */
	public static void sendEventOnSelect(Control control, String topic, String key, Object value) {
		Map<String, Object> properties = new HashMap<>();
		properties.put(key, value);
		sendEventOnSelect(control, topic, properties);
	}

	/**
	 * The CMS view related to this display, or null if none is available from this
	 * call.
	 * 
	 * @deprecated Use {@link CmsView#getCmsView(Composite)} instead.
	 */
	@Deprecated
	public static CmsView getCmsView() {
//		return UiContext.getData(CmsView.class.getName());
		return CmsView.getCmsView(Display.getCurrent().getActiveShell());
	}

	public static StringBuilder getServerBaseUrl(HttpServletRequest request) {
		try {
			URL url = new URL(request.getRequestURL().toString());
			StringBuilder buf = new StringBuilder();
			buf.append(url.getProtocol()).append("://").append(url.getHost());
			if (url.getPort() != -1)
				buf.append(':').append(url.getPort());
			return buf;
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Cannot extract server base URL from " + request.getRequestURL(), e);
		}
	}

	//
	public static String getDataUrl(Node node, HttpServletRequest request) throws RepositoryException {
		try {
			StringBuilder buf = getServerBaseUrl(request);
			buf.append(getDataPath(node));
			return new URL(buf.toString()).toString();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Cannot build data URL for " + node, e);
		}
	}

	/** A path in the node repository */
	public static String getDataPath(Node node) throws RepositoryException {
		return getDataPath(NodeConstants.EGO_REPOSITORY, node);
	}

	public static String getDataPath(String cn, Node node) throws RepositoryException {
		return NodeUtils.getDataPath(cn, node);
	}

	/** @deprecated Use rowData16px() instead. GridData should not be reused. */
	@Deprecated
	public static RowData ROW_DATA_16px = new RowData(16, 16);

	/*
	 * GRID LAYOUT
	 */
	public static GridLayout noSpaceGridLayout() {
		return noSpaceGridLayout(new GridLayout());
	}

	public static GridLayout noSpaceGridLayout(int columns) {
		return noSpaceGridLayout(new GridLayout(columns, false));
	}

	public static GridLayout noSpaceGridLayout(GridLayout layout) {
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		return layout;
	}

	public static GridData fillAll() {
		return new GridData(SWT.FILL, SWT.FILL, true, true);
	}

	public static GridData fillWidth() {
		return grabWidth(SWT.FILL, SWT.FILL);
	}

	public static GridData grabWidth(int horizontalAlignment, int verticalAlignment) {
		return new GridData(horizontalAlignment, horizontalAlignment, true, false);
	}

	public static GridData fillHeight() {
		return grabHeight(SWT.FILL, SWT.FILL);
	}

	public static GridData grabHeight(int horizontalAlignment, int verticalAlignment) {
		return new GridData(horizontalAlignment, horizontalAlignment, false, true);
	}

	public static RowData rowData16px() {
		return new RowData(16, 16);
	}

	/*
	 * FORM LAYOUT
	 */

	public static FormData coverAll() {
		FormData fdLabel = new FormData();
		fdLabel.top = new FormAttachment(0, 0);
		fdLabel.left = new FormAttachment(0, 0);
		fdLabel.right = new FormAttachment(100, 0);
		fdLabel.bottom = new FormAttachment(100, 0);
		return fdLabel;
	}

	/*
	 * STYLING
	 */

	/** Style widget */
	public static <T extends Widget> T style(T widget, String style) {
		if (style == null)
			return widget;// does nothing
		EclipseUiSpecificUtils.setStyleData(widget, style);
		if (widget instanceof Control) {
			CmsView.getCmsView((Control) widget).applyStyles(widget);
		}
		return widget;
	}

	/** Style widget */
	public static <T extends Widget> T style(T widget, CmsStyle style) {
		return style(widget, style.toStyleClass());
	}

	/** Enable markups on widget */
	public static <T extends Widget> T markup(T widget) {
		EclipseUiSpecificUtils.setMarkupData(widget);
		return widget;
	}

	/**
	 * Apply markup and set text on {@link Label}, {@link Button}, {@link Text}.
	 * 
	 * @param widget the widget to style and to use in order to display text
	 * @param txt    the object to display via its <code>toString()</code> method.
	 *               This argument should not be null, but if it is null and
	 *               assertions are disabled "<null>" is displayed instead; if
	 *               assertions are enabled the call will fail.
	 * 
	 * @see #markup(Widget)
	 */
	public static <T extends Widget> T text(T widget, Object txt) {
		assert txt != null;
		String str = txt != null ? txt.toString() : "<null>";
		markup(widget);
		if (widget instanceof Label)
			((Label) widget).setText(str);
		else if (widget instanceof Button)
			((Button) widget).setText(str);
		else if (widget instanceof Text)
			((Text) widget).setText(str);
		else
			throw new IllegalArgumentException("Unsupported widget type " + widget.getClass());
		return widget;
	}

	/** A {@link Label} with markup activated. */
	public static Label lbl(Composite parent, Object txt) {
		return text(new Label(parent, SWT.NONE), txt);
	}

	/** A read-only {@link Text} whose content can be copy/pasted. */
	public static Text txt(Composite parent, Object txt) {
		return text(new Text(parent, SWT.NONE), txt);
	}

	@Deprecated
	public static void setItemHeight(Table table, int height) {
		table.setData(CmsConstants.ITEM_HEIGHT, height);
	}

	/** Dispose all children of a Composite */
	public static void clear(Composite composite) {
		for (Control child : composite.getChildren())
			child.dispose();
	}

	//
	// JCR
	//
	public static Node getOrAddEmptyFile(Node parent, Enum<?> child) throws RepositoryException {
		if (has(parent, child))
			return child(parent, child);
		return JcrUtils.copyBytesAsFile(parent, child.name(), new byte[0]);
	}

	public static Node child(Node parent, Enum<?> en) throws RepositoryException {
		return parent.getNode(en.name());
	}

	public static Boolean has(Node parent, Enum<?> en) throws RepositoryException {
		return parent.hasNode(en.name());
	}

	public static Node getOrAdd(Node parent, Enum<?> en) throws RepositoryException {
		return getOrAdd(parent, en, null);
	}

	public static Node getOrAdd(Node parent, Enum<?> en, String primaryType) throws RepositoryException {
		if (has(parent, en))
			return child(parent, en);
		else if (primaryType == null)
			return parent.addNode(en.name());
		else
			return parent.addNode(en.name(), primaryType);
	}

	// IMAGES
	public static String img(String src, String width, String height) {
		return imgBuilder(src, width, height).append("/>").toString();
	}

	public static String img(String src, Point size) {
		return img(src, Integer.toString(size.x), Integer.toString(size.y));
	}

	public static StringBuilder imgBuilder(String src, String width, String height) {
		return new StringBuilder(64).append("<img width='").append(width).append("' height='").append(height)
				.append("' src='").append(src).append("'");
	}

	public static String noImg(Point size) {
		ResourceManager rm = RWT.getResourceManager();
		return CmsUiUtils.img(rm.getLocation(NO_IMAGE), size);
	}

	public static String noImg() {
		return noImg(NO_IMAGE_SIZE);
	}

	public static Image noImage(Point size) {
		ResourceManager rm = RWT.getResourceManager();
		InputStream in = null;
		try {
			in = rm.getRegisteredContent(NO_IMAGE);
			ImageData id = new ImageData(in);
			ImageData scaled = id.scaledTo(size.x, size.y);
			Image image = new Image(Display.getCurrent(), scaled);
			return image;
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				// silent
			}
		}
	}

	/** Lorem ipsum text to be used during development. */
	public final static String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
			+ " Etiam eleifend hendrerit sem, ac ultricies massa ornare ac."
			+ " Cras aliquam sodales risus, vitae varius lacus molestie quis."
			+ " Vivamus consequat, leo id lacinia volutpat, eros diam efficitur urna, finibus interdum risus turpis at nisi."
			+ " Curabitur vulputate nulla quis scelerisque fringilla. Integer consectetur turpis id lobortis accumsan."
			+ " Pellentesque commodo turpis ac diam ultricies dignissim."
			+ " Curabitur sit amet dolor volutpat lacus aliquam ornare quis sed velit."
			+ " Integer varius quis est et tristique."
			+ " Suspendisse pharetra porttitor purus, eget condimentum magna."
			+ " Duis vitae turpis eros. Sed tincidunt lacinia rutrum."
			+ " Aliquam velit velit, rutrum ut augue sed, condimentum lacinia augue.";

	/** Singleton. */
	private CmsUiUtils() {
	}
}
