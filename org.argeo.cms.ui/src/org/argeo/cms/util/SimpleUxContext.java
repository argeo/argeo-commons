package org.argeo.cms.util;

import org.argeo.cms.UxContext;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class SimpleUxContext implements UxContext {
	private Point size;
	private Point small = new Point(400, 400);

	public SimpleUxContext() {
		this(Display.getCurrent().getBounds());
	}

	public SimpleUxContext(Rectangle rect) {
		this.size = new Point(rect.width, rect.height);
	}

	public SimpleUxContext(Point size) {
		this.size = size;
	}

	@Override
	public boolean isPortrait() {
		return size.x >= size.y;
	}

	@Override
	public boolean isLandscape() {
		return size.x < size.y;
	}

	@Override
	public boolean isSquare() {
		return size.x == size.y;
	}

	@Override
	public boolean isSmall() {
		return size.x <= small.x || size.y <= small.y;
	}

}
