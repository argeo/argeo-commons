package org.argeo.eclipse.ui.specific;

import org.eclipse.jface.viewers.AbstractTableViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.Viewer;

/** Static utilities to bridge differences between RCP and RAP */
public class EclipseUiSpecificUtils {

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
