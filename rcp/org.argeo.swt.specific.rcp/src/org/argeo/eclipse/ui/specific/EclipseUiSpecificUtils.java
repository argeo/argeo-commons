package org.argeo.eclipse.ui.specific;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Widget;

/** Static utilities to bridge differences between RCP and RAP */
public class EclipseUiSpecificUtils {
	private final static String CSS_CLASS = "org.eclipse.e4.ui.css.CssClassName";

	public static void setStyleData(Widget widget, Object data) {
		widget.setData(CSS_CLASS, data);
	}

	public static Object getStyleData(Widget widget) {
		return widget.getData(CSS_CLASS);
	}

	public static void setMarkupData(Widget widget) {
		// does nothing
	}

	public static void setMarkupValidationDisabledData(Widget widget) {
		// does nothing
	}

	/**
	 * TootlTip support is supported for {@link ColumnViewer} in RCP
	 * 
	 * @see ColumnViewerToolTipSupport#enableFor(Viewer)
	 */
	public static void enableToolTipSupport(Viewer viewer) {
		if (viewer instanceof ColumnViewer)
			ColumnViewerToolTipSupport.enableFor((ColumnViewer) viewer);
	}

	private EclipseUiSpecificUtils() {
	}
}
