package org.argeo.api.cms.ux;

/** A 2D size. */
public class Cms2DSize {
	private Integer width;
	private Integer height;

	public Cms2DSize() {
	}

	public Cms2DSize(Integer width, Integer height) {
		super();
		this.width = width;
		this.height = height;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	@Override
	public String toString() {
		return Cms2DSize.class.getSimpleName() + "[" + width + "," + height + "]";
	}

}
