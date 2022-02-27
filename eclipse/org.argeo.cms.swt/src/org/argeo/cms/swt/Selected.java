package org.argeo.cms.swt;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

/**
 * {@link SelectionListener} as a functional interface in order to use as a
 * short lambda expression in UI code.
 * {@link SelectionListener#widgetDefaultSelected(SelectionEvent)} does nothing
 * by default.
 */
@FunctionalInterface
public interface Selected extends SelectionListener {
	@Override
	public void widgetSelected(SelectionEvent e);

	default public void widgetDefaultSelected(SelectionEvent e) {
		// does nothing
	}

}
