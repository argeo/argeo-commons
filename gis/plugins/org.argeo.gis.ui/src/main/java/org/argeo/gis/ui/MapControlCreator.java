package org.argeo.gis.ui;

import org.eclipse.swt.widgets.Composite;

public interface MapControlCreator {
	/** Creates a map control based on this parent and this map context */
	public Composite createMapControl(Composite parent,
			MapContextProvider mapContextProvider);
}
