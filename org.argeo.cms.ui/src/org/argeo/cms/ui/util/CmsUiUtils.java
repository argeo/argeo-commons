package org.argeo.cms.ui.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.argeo.api.NodeConstants;
import org.argeo.api.cms.Cms2DSize;
import org.argeo.api.cms.CmsView;
import org.argeo.cms.jcr.CmsJcrUtils;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ui.CmsConstants;
import org.argeo.jcr.JcrUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.service.ResourceManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;

/** Static utilities for the CMS framework. */
public class CmsUiUtils {
	// private final static Log log = LogFactory.getLog(CmsUiUtils.class);

	/*
	 * CMS VIEW
	 */

	/**
	 * The CMS view related to this display, or null if none is available from this
	 * call.
	 * 
	 * @deprecated Use {@link CmsSwtUtils#getCmsView(Composite)} instead.
	 */
	@Deprecated
	public static CmsView getCmsView() {
//		return UiContext.getData(CmsView.class.getName());
		return CmsSwtUtils.getCmsView(Display.getCurrent().getActiveShell());
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
	public static String getDataUrl(Node node, HttpServletRequest request) {
		try {
			StringBuilder buf = getServerBaseUrl(request);
			buf.append(getDataPath(node));
			return new URL(buf.toString()).toString();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Cannot build data URL for " + node, e);
		}
	}

	/** A path in the node repository */
	public static String getDataPath(Node node) {
		return getDataPath(NodeConstants.EGO_REPOSITORY, node);
	}

	public static String getDataPath(String cn, Node node) {
		return CmsJcrUtils.getDataPath(cn, node);
	}

	/** Clean reserved URL characters for use in HTTP links. */
	public static String getDataPathForUrl(Node node) {
		return cleanPathForUrl(getDataPath(node));
	}

	/** Clean reserved URL characters for use in HTTP links. */
	public static String cleanPathForUrl(String path) {
		StringTokenizer st = new StringTokenizer(path, "/");
		StringBuilder sb = new StringBuilder();
		while (st.hasMoreElements()) {
			sb.append('/');
			String encoded = URLEncoder.encode(st.nextToken(), StandardCharsets.UTF_8);
			encoded = encoded.replace("+", "%20");
			sb.append(encoded);

		}
		return sb.toString();
	}

	/** @deprecated Use rowData16px() instead. GridData should not be reused. */
	@Deprecated
	public static RowData ROW_DATA_16px = new RowData(16, 16);

	

	/*
	 * FORM LAYOUT
	 */

	

	@Deprecated
	public static void setItemHeight(Table table, int height) {
		table.setData(CmsConstants.ITEM_HEIGHT, height);
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

	public static String img(Node fileNode, String width, String height) {
		return img(null, fileNode, width, height);
	}

	public static String img(String serverBase, Node fileNode, String width, String height) {
//		String src = (serverBase != null ? serverBase : "") + NodeUtils.getDataPath(fileNode);
		String src;
		src = (serverBase != null ? serverBase : "") + getDataPathForUrl(fileNode);
		return imgBuilder(src, width, height).append("/>").toString();
	}

	public static String img(String src, String width, String height) {
		return imgBuilder(src, width, height).append("/>").toString();
	}

	public static String img(String src, Cms2DSize size) {
		return img(src, Integer.toString(size.getWidth()), Integer.toString(size.getHeight()));
	}

	public static StringBuilder imgBuilder(String src, String width, String height) {
		return new StringBuilder(64).append("<img width='").append(width).append("' height='").append(height)
				.append("' src='").append(src).append("'");
	}

	public static String noImg(Cms2DSize size) {
		ResourceManager rm = RWT.getResourceManager();
		return CmsUiUtils.img(rm.getLocation(CmsConstants.NO_IMAGE), size);
	}

	public static String noImg() {
		return noImg(CmsConstants.NO_IMAGE_SIZE);
	}

	public static Image noImage(Cms2DSize size) {
		ResourceManager rm = RWT.getResourceManager();
		InputStream in = null;
		try {
			in = rm.getRegisteredContent(CmsConstants.NO_IMAGE);
			ImageData id = new ImageData(in);
			ImageData scaled = id.scaledTo(size.getWidth(), size.getHeight());
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
