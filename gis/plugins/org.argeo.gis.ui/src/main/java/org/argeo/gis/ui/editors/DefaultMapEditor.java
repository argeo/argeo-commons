package org.argeo.gis.ui.editors;

import org.argeo.gis.ui.MapContextProvider;
import org.argeo.gis.ui.MapControlCreator;
import org.argeo.gis.ui.views.LayersView;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.geotools.data.FeatureSource;
import org.geotools.map.MapContext;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/** A generic map editor */
public class DefaultMapEditor extends EditorPart implements MapContextProvider {
	public final static String ID = "org.argeo.gis.ui.defaultMapEditor";

	private MapContext mapContext;
	private Composite map;
	private MapControlCreator mapControlCreator;

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (input instanceof MapContextProvider) {
			mapContext = ((MapContextProvider) input).getMapContext();
			setSite(site);
			setInput(input);
			setPartName(input.getName());
		} else {
			throw new PartInitException("Support only "
					+ MapContextProvider.class + " inputs");
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite mapArea = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		mapArea.setLayout(layout);
		map = mapControlCreator.createMapControl(mapArea, this);
	}

	public void addLayer(
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {
		// TODO: deal with style
		mapContext.addLayer(featureSource, null);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setFocus() {
		LayersView layersView = (LayersView) getEditorSite()
				.getWorkbenchWindow().getActivePage().findView(LayersView.ID);
		layersView.setMapContext(getMapContext());
		map.setFocus();
	}

	public MapContext getMapContext() {
		return mapContext;
	}

	public void setMapControlCreator(MapControlCreator mapControlCreator) {
		this.mapControlCreator = mapControlCreator;
	}

}
