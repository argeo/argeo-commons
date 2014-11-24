package org.argeo.cms;

import java.io.InputStream;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.argeo.jcr.JcrUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.service.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

/** Static utilities for the CMS framework. */
public class CmsUtils implements CmsConstants {
	/** @deprecated Use rowData16px() instead. GridData should not be reused. */
	@Deprecated
	public static RowData ROW_DATA_16px = new RowData(16, 16);

	public static GridLayout noSpaceGridLayout() {
		return noSpaceGridLayout(new GridLayout());
	}

	public static GridLayout noSpaceGridLayout(GridLayout layout) {
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		return layout;
	}

	//
	// GRID DATA
	//
	public static GridData fillWidth() {
		return grabWidth(SWT.FILL, SWT.FILL);
	}

	public static GridData fillAll() {
		return new GridData(SWT.FILL, SWT.FILL, true, true);
	}

	public static GridData grabWidth(int horizontalAlignment,
			int verticalAlignment) {
		return new GridData(horizontalAlignment, horizontalAlignment, true,
				false);
	}

	public static RowData rowData16px() {
		return new RowData(16, 16);
	}

	public static void style(Widget widget, String style) {
		widget.setData(CmsConstants.STYLE, style);
	}

	public static void markup(Widget widget) {
		widget.setData(CmsConstants.MARKUP, true);
	}

	/** @return the path or null if not instrumented */
	public static String getDataPath(Widget widget) {
		// JCR item
		Object data = widget.getData();
		if (data != null && data instanceof Item) {
			try {
				return ((Item) data).getPath();
			} catch (RepositoryException e) {
				throw new CmsException("Cannot find data path of " + data
						+ " for " + widget);
			}
		}

		// JCR path
		data = widget.getData(Property.JCR_PATH);
		if (data != null)
			return data.toString();

		return null;
	}

	/** Dispose all children of a Composite */
	public static void clear(Composite composite) {
		for (Control child : composite.getChildren())
			child.dispose();
	}

	//
	// JCR
	//
	public static Node getOrAddEmptyFile(Node parent, Enum<?> child)
			throws RepositoryException {
		if (has(parent, child))
			return child(parent, child);
		return JcrUtils.copyBytesAsFile(parent, child.name(), new byte[0]);
	}

	public static Node child(Node parent, Enum<?> en)
			throws RepositoryException {
		return parent.getNode(en.name());
	}

	public static Boolean has(Node parent, Enum<?> en)
			throws RepositoryException {
		return parent.hasNode(en.name());
	}

	public static Node getOrAdd(Node parent, Enum<?> en)
			throws RepositoryException {
		return getOrAdd(parent, en, null);
	}

	public static Node getOrAdd(Node parent, Enum<?> en, String primaryType)
			throws RepositoryException {
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

	public static StringBuilder imgBuilder(String src, String width,
			String height) {
		return new StringBuilder(64).append("<img width='").append(width)
				.append("' height='").append(height).append("' src='")
				.append(src).append("'");
	}

	public static String noImg(Point size) {
		ResourceManager rm = RWT.getResourceManager();
		return CmsUtils.img(rm.getLocation(NO_IMAGE), size);
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
			IOUtils.closeQuietly(in);
		}
	}

	private CmsUtils() {
	}
}
