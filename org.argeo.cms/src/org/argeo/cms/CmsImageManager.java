package org.argeo.cms;

import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

/** Read and write access to images. */
public interface CmsImageManager {
	/** Load image in control */
	public Boolean load(Node node, Control control, Point size)
			throws RepositoryException;

	/** @return (0,0) if not available */
	public Point getImageSize(Node node) throws RepositoryException;

	/**
	 * The related <img tag, with src, width and height set. @return null if not
	 * available
	 */
	public String getImageTag(Node node) throws RepositoryException;

	/**
	 * The related <img tag, with url, width and height set. Caller must close
	 * the tag (or add additional attributes). @return null if not available
	 */
	public StringBuilder getImageTagBuilder(Node node, Point size)
			throws RepositoryException;

	/**
	 * Returns the remotely accessible URL of the image (registering it if
	 * needed) @return null if not available
	 */
	public String getImageUrl(Node node) throws RepositoryException;

	public Binary getImageBinary(Node node) throws RepositoryException;

	public Image getSwtImage(Node node) throws RepositoryException;

	/** @return URL */
	public String uploadImage(Node parentNode, String fileName, InputStream in)
			throws RepositoryException;
}
