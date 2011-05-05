package org.argeo.gis.ui.rcp.swing;

import java.awt.Frame;

import javax.jcr.Node;

import org.argeo.geotools.jcr.GeoJcrMapper;
import org.argeo.gis.ui.AbstractMapViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.geotools.data.FeatureSource;
import org.geotools.map.DefaultMapContext;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.swing.JMapPane;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class SwingMapViewer extends AbstractMapViewer {
	private Composite embedded;
	private JMapPane mapPane;
	private VersatileZoomTool versatileZoomTool;

	public SwingMapViewer(Node context, GeoJcrMapper geoJcrMapper,
			Composite parent) {
		super(context, geoJcrMapper);

		embedded = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		Frame frame = SWT_AWT.new_Frame(embedded);

		mapPane = new JMapPane(new StreamingRenderer(), new DefaultMapContext());
		versatileZoomTool = new VersatileZoomTool();
		mapPane.setCursorTool(versatileZoomTool);

		frame.add(mapPane);

		setControl(embedded);
	}

	@Override
	protected void addFeatureSource(String path,
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {
		// TODO: deal with style and rasters
		mapPane.getMapContext().addLayer(featureSource, null);
	}

}
