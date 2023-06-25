package org.argeo.api.cms.ux;

import java.io.InputStream;
import java.net.URI;

/** Read and write access to images. */
public interface CmsImageManager<V, M> {
	/** Load image in control */
	public Boolean load(M node, V control, Cms2DSize size, URI link);

	/** @return (0,0) if not available */
	public Cms2DSize getImageSize(M node);

	/**
	 * The related &lt;img&gt; tag, with src, width and height set.
	 * 
	 * @return null if not available
	 */
	public String getImageTag(M node);

	/**
	 * The related &lt;img&gt; tag, with url, width and height set. Caller must
	 * close the tag (or add additional attributes).
	 * 
	 * @return null if not available
	 */
	public StringBuilder getImageTagBuilder(M node, Cms2DSize size);

	/**
	 * Returns the remotely accessible URL of the image (registering it if
	 * needed) @return null if not available
	 */
	public String getImageUrl(M node);

//	public Binary getImageBinary(Node node) throws RepositoryException;

//	public Image getSwtImage(Node node) throws RepositoryException;

	/** @return URL */
	public String uploadImage(M context, M uploadFolder, String fileName, InputStream in, String contentType);

	@Deprecated
	default String uploadImage(M uploadFolder, String fileName, InputStream in) {
		System.err.println("Context must be provided to " + CmsImageManager.class.getName());
		return uploadImage(null, uploadFolder, fileName, in, null);
	}
}
