package org.argeo.gis.ui.rcp.swing;

import javax.jcr.Node;

import org.argeo.geotools.jcr.GeoJcrMapper;
import org.argeo.gis.ui.MapControlCreator;
import org.argeo.gis.ui.MapViewer;
import org.eclipse.swt.widgets.Composite;

/** Creates a Swing map viewer */
public class SwingMapControlCreator implements MapControlCreator {
	private GeoJcrMapper geoJcrMapper;

	public MapViewer createMapControl(Node context, Composite parent) {
		return new SwingMapViewer(context, geoJcrMapper, parent);
	}

	public void setGeoJcrMapper(GeoJcrMapper geoJcrMapper) {
		this.geoJcrMapper = geoJcrMapper;
	}

}
