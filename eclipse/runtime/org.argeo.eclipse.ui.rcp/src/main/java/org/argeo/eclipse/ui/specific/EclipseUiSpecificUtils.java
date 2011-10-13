package org.argeo.eclipse.ui.specific;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.Viewer;

/** Static utilities to bridge differences between RCP and RAP */
public class EclipseUiSpecificUtils {
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
