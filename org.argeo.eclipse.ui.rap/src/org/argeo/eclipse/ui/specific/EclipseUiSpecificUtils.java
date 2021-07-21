package org.argeo.eclipse.ui.specific;

import org.eclipse.jface.viewers.AbstractTableViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.widgets.Widget;

/** Static utilities to bridge differences between RCP and RAP */
public class EclipseUiSpecificUtils {

	public static void setStyleData(Widget widget, Object data) {
		widget.setData(RWT.CUSTOM_VARIANT, data);
	}

	public static Object getStyleData(Widget widget) {
		return widget.getData(RWT.CUSTOM_VARIANT);
	}

	public static void setMarkupData(Widget widget) {
		widget.setData(RWT.MARKUP_ENABLED, true);
	}

	public static void setMarkupValidationDisabledData(Widget widget) {
		widget.setData("org.eclipse.rap.rwt.markupValidationDisabled", Boolean.TRUE);
	}

	/**
	 * TootlTip support is supported only for {@link AbstractTableViewer} in RAP
	 */
	public static void enableToolTipSupport(Viewer viewer) {
		if (viewer instanceof ColumnViewer)
			ColumnViewerToolTipSupport.enableFor((ColumnViewer) viewer);
	}

	private EclipseUiSpecificUtils() {
	}
}
