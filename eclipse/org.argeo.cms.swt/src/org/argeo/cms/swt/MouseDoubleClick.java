package org.argeo.cms.swt;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;

/**
 * {@link MouseListener#mouseDoubleClick(MouseEvent)} as a functional interface
 * in order to use as a short lambda expression in UI code.
 * {@link MouseListener#mouseDownouseEvent)} and
 * {@link MouseListener#mouseUp(MouseEvent)} do nothing by default.
 */
@FunctionalInterface
public interface MouseDoubleClick extends MouseListener {
	@Override
	void mouseDoubleClick(MouseEvent e);

	@Override
	default void mouseDown(MouseEvent e) {
		// does nothing
	}

	@Override
	default void mouseUp(MouseEvent e) {
		// does nothing
	}
}
