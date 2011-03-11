package org.argeo.gis.ui.rap.openlayers;

import javax.jcr.Node;

import org.argeo.geotools.jcr.GeoJcrMapper;
import org.argeo.gis.ui.MapControlCreator;
import org.argeo.gis.ui.MapViewer;
import org.eclipse.swt.widgets.Composite;

public class OpenLayersMapControlCreator implements MapControlCreator {
	private GeoJcrMapper geoJcrMapper;
	
	public MapViewer createMapControl(Node context,Composite parent) {
		return new OpenLayersMapViewer(context,geoJcrMapper,parent);
	}

	public void setGeoJcrMapper(GeoJcrMapper geoJcrMapper) {
		this.geoJcrMapper = geoJcrMapper;
	}

	
}
