package org.argeo.cms.swt;

import org.argeo.api.cms.UxContext;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class SimpleSwtUxContext implements UxContext {
	private Point size;
	private Point small = new Point(400, 400);

	public SimpleSwtUxContext() {
		this(Display.getCurrent().getBounds());
	}

	public SimpleSwtUxContext(Rectangle rect) {
		this.size = new Point(rect.width, rect.height);
	}

	public SimpleSwtUxContext(Point size) {
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

	@Override
	public boolean isMasterData() {
		// TODO make it configurable
		return true;
	}

}
