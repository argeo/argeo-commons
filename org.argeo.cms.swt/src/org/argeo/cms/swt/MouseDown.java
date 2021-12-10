package org.argeo.cms.swt;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;

/**
 * {@link MouseListener#mouseDown(MouseEvent)} as a functional interface in
 * order to use as a short lambda expression in UI code.
 * {@link MouseListener#mouseDoubleClick(MouseEvent)} and
 * {@link MouseListener#mouseUp(MouseEvent)} do nothing by default.
 */
@FunctionalInterface
public interface MouseDown extends MouseListener {
	@Override
	void mouseDown(MouseEvent e);

	@Override
	default void mouseDoubleClick(MouseEvent e) {
		// does nothing
	}

	@Override
	default void mouseUp(MouseEvent e) {
		// does nothing
	}
}
