package org.argeo.eclipse.ui.specific;

import org.eclipse.jface.viewers.AbstractTableViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.Viewer;

/** Static utilities to bridge differences between RCP and RAP */
public class EclipseUiSpecificUtils {
	/**
	 * TootlTip support is supported only for {@link AbstractTableViewer} in RAP
	 * 
	 * @see ColumnViewerToolTipSupport#enableFor(AbstractTableViewer)
	 */
	public static void enableToolTipSupport(Viewer viewer) {
		if (viewer instanceof AbstractTableViewer)
			ColumnViewerToolTipSupport.enableFor((AbstractTableViewer) viewer);
	}

	private EclipseUiSpecificUtils() {
	}

}
