package org.argeo.gis.ui.rcp.swing;

import java.awt.Color;
import java.awt.Frame;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.argeo.ArgeoException;
import org.argeo.geotools.jcr.GeoJcrMapper;
import org.argeo.geotools.styling.StylingUtils;
import org.argeo.gis.ui.AbstractMapViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.DefaultMapLayer;
import org.geotools.map.MapContext;
import org.geotools.map.MapLayer;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;
import org.geotools.swing.JMapPane;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** Map viewer implementation based on GeoTools Swing components. */
public class SwingMapViewer extends AbstractMapViewer {
	private Composite embedded;
	private JMapPane mapPane;
	private VersatileZoomTool versatileZoomTool;

	private Map<String, MapLayer> mapLayers = Collections
			.synchronizedMap(new HashMap<String, MapLayer>());

	public SwingMapViewer(Node context, GeoJcrMapper geoJcrMapper,
			Composite parent) {
		super(context, geoJcrMapper);

		embedded = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		Frame frame = SWT_AWT.new_Frame(embedded);

		MapContext mapContext = new DefaultMapContext();
		// dummy call to make sure that the layers are initialized
		mapContext.layers();
		mapPane = new JMapPane(new StreamingRenderer(), mapContext);
		versatileZoomTool = new VersatileZoomTool();
		mapPane.setCursorTool(versatileZoomTool);
		mapPane.setBackground(Color.WHITE);
		frame.add(mapPane);

		setControl(embedded);
	}

	@Override
	protected void addFeatureSource(String layerId,
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource,
			Object style) {
		if (style == null)
			style = StylingUtils.createLineStyle("BLACK", 1);

		MapLayer mapLayer = new DefaultMapLayer(featureSource, (Style) style);
		addMapLayer(layerId, mapLayer);
	}

	protected void addMapLayer(String layerId, MapLayer mapLayer) {
		mapLayers.put(layerId, mapLayer);
		mapPane.getMapContext().addLayer(mapLayer);
	}

	public void addLayer(String layerId, Collection<?> collection, Object style) {
		if (style == null)
			style = StylingUtils.createLineStyle("BLACK", 1);
		MapLayer mapLayer = new DefaultMapLayer(collection, (Style) style);
		addMapLayer(layerId, mapLayer);
	}

	public void setStyle(String layerId, Object style) {
		mapLayers.get(layerId).setStyle((Style) style);
	}

	public void setAreaOfInterest(ReferencedEnvelope areaOfInterest) {
		// mapPane.getMapContext().setAreaOfInterest(areaOfInterest);
		CoordinateReferenceSystem crs = mapPane.getMapContext()
				.getCoordinateReferenceSystem();

		ReferencedEnvelope toDisplay;
		if (crs != null)
			try {
				toDisplay = areaOfInterest.transform(crs, true);
			} catch (Exception e) {
				throw new ArgeoException("Cannot reproject " + areaOfInterest,
						e);
			}
		else
			toDisplay = areaOfInterest;
		mapPane.setDisplayArea(toDisplay);
	}

	public void setCoordinateReferenceSystem(String crs) {
		try {
			CoordinateReferenceSystem crsObj = CRS.decode(crs);
			mapPane.getMapContext().setCoordinateReferenceSystem(crsObj);
			mapPane.repaint();
		} catch (Exception e) {
			throw new ArgeoException("Cannot set CRS '" + crs + "'", e);
		}

	}

}
