package org.argeo.cms.swt.acr;

import org.argeo.api.acr.Content;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * Creates SWT UI parts based on a given view/model context. For simple UIs, the
 * view and controller can usually be implemented directly in the method, while
 * more complex UI components may require a dedicated object (either an internal
 * or external class). The main purpose of this factory pattern is to ease the
 * dependency injection of other services used.
 */
@FunctionalInterface
public interface SwtUiProvider {
	/**
	 * Populates the provided {@link Composite} with SWT UI components (view) and
	 * listeners (controller), based on the provided {@link Content}. For a typical
	 * view or editor, the context will be the data to display/edit, but it can also
	 * just be used to access the underlying data session.
	 * 
	 * @param parent  the SWT {@link Composite} to use as parent for the widgets.
	 *                Implementations should not assume that a {@link Layout} has
	 *                been set on it, and should therefore set the appropriate
	 *                layout themselves.
	 * @param context the data to display or a generic data context (typically a
	 *                user home area).
	 * @return a {@link Control} within the parent on which to focus if needed, Can
	 *         be null, so client should always check.
	 */
	Control createUiPart(Composite parent, Content context);
}
