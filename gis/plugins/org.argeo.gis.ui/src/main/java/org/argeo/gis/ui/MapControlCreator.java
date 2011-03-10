package org.argeo.gis.ui;

import javax.jcr.Node;

import org.eclipse.swt.widgets.Composite;

public interface MapControlCreator {
	/** Creates a map control based on this parent and this context */
	public MapViewer createMapControl(Node context, Composite parent);
}
