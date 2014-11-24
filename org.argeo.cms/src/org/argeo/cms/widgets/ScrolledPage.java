package org.argeo.cms.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

/**
 * A composite that can be scrolled vertically. It wraps a
 * {@link ScrolledComposite} (and is being wrapped by it), simplifying its
 * configuration.
 */
public class ScrolledPage extends Composite {
	private static final long serialVersionUID = 1593536965663574437L;

	private ScrolledComposite scrolledComposite;

	public ScrolledPage(Composite parent, int style) {
		super(new ScrolledComposite(parent, SWT.V_SCROLL), style);
		scrolledComposite = (ScrolledComposite) getParent();
		scrolledComposite.setContent(this);

		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.addControlListener(new ScrollControlListener());
	}

	@Override
	public void layout(boolean changed, boolean all) {
		updateScroll();
		super.layout(changed, all);
	}

	protected void updateScroll() {
		Rectangle r = scrolledComposite.getClientArea();
		Point preferredSize = computeSize(r.width, SWT.DEFAULT);
		scrolledComposite.setMinHeight(preferredSize.y);
	}

	// public ScrolledComposite getScrolledComposite() {
	// return this.scrolledComposite;
	// }

	/** Set it on the wrapping scrolled composite */
	@Override
	public void setLayoutData(Object layoutData) {
		scrolledComposite.setLayoutData(layoutData);
	}

	private class ScrollControlListener extends
			org.eclipse.swt.events.ControlAdapter {
		private static final long serialVersionUID = -3586986238567483316L;

		public void controlResized(ControlEvent e) {
			updateScroll();
		}
	}
}
