package org.argeo.api.cms.ux;

/** A 2D size. */
public record Cms2DSize(int width, int height) {
	@Override
	public String toString() {
		return Cms2DSize.class.getSimpleName() + "[" + width + "," + height + "]";
	}
}
