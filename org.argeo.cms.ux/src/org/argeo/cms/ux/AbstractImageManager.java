package org.argeo.cms.ux;

import org.argeo.api.cms.ux.Cms2DSize;
import org.argeo.api.cms.ux.CmsImageManager;

/** Manages only public images so far. */
public abstract class AbstractImageManager<V, M> implements CmsImageManager<V, M> {
	public final static String NO_IMAGE = "icons/noPic-square-640px.png";
	public final static Cms2DSize NO_IMAGE_SIZE = new Cms2DSize(320, 320);
	public final static Float NO_IMAGE_RATIO = 1f;

	protected Cms2DSize resizeTo(Cms2DSize orig, Cms2DSize constraints) {
		if (constraints.getWidth() != 0 && constraints.getHeight() != 0) {
			return constraints;
		} else if (constraints.getWidth() == 0 && constraints.getHeight() == 0) {
			return orig;
		} else if (constraints.getHeight() == 0) {// force width
			return new Cms2DSize(constraints.getWidth(),
					scale(orig.getHeight(), orig.getWidth(), constraints.getWidth()));
		} else if (constraints.getWidth() == 0) {// force height
			return new Cms2DSize(scale(orig.getWidth(), orig.getHeight(), constraints.getHeight()),
					constraints.getHeight());
		}
		throw new IllegalArgumentException("Cannot resize " + orig + " to " + constraints);
	}

	protected int scale(int origDimension, int otherDimension, int otherConstraint) {
		return Math.round(origDimension * divide(otherConstraint, otherDimension));
	}

	protected float divide(int a, int b) {
		return ((float) a) / ((float) b);
	}

	/** @return null if not available */
	@Override
	public String getImageTag(M node) {
		return getImageTag(node, getImageSize(node));
	}

	protected String getImageTag(M node, Cms2DSize size) {
		StringBuilder buf = getImageTagBuilder(node, size);
		if (buf == null)
			return null;
		return buf.append("/>").toString();
	}

	/** @return null if not available */
	@Override
	public StringBuilder getImageTagBuilder(M node, Cms2DSize size) {
		return getImageTagBuilder(node, Integer.toString(size.getWidth()), Integer.toString(size.getHeight()));
	}

	/** @return null if not available */
	protected StringBuilder getImageTagBuilder(M node, String width, String height) {
		String url = getImageUrl(node);
		if (url == null)
			return null;
		return CmsUxUtils.imgBuilder(url, width, height);
	}

}
