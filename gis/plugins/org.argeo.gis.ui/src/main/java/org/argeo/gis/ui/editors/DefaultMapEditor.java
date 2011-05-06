package org.argeo.gis.ui.editors;

import javax.jcr.Node;

import org.argeo.gis.ui.MapControlCreator;
import org.argeo.gis.ui.MapViewer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

/** A generic map editor */
public class DefaultMapEditor extends EditorPart {
	private Node context;
	private MapViewer mapViewer;
	private MapControlCreator mapControlCreator;

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (input instanceof MapEditorInput) {
			// mapContext = ((MapEditorInput) input).getMapContext();
			context = ((MapEditorInput) input).getContext();
			setSite(site);
			setInput(input);
			setPartName(input.getName());
		} else {
			throw new PartInitException("Support only " + MapEditorInput.class
					+ " inputs");
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite mapArea = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		mapArea.setLayout(layout);
		mapViewer = mapControlCreator.createMapControl(context, mapArea);
		mapViewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	public MapViewer getMapViewer() {
		return mapViewer;
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
		// LayersView layersView = (LayersView) getEditorSite()
		// .getWorkbenchWindow().getActivePage().findView(LayersView.ID);
		// layersView.setMapContext(getMapContext());
		mapViewer.getControl().setFocus();
	}

	public void featureSelected(String layerId, String featureId) {
		// TODO Auto-generated method stub

	}

	public void featureUnselected(String layerId, String featureId) {
		// TODO Auto-generated method stub

	}

	public void setMapControlCreator(MapControlCreator mapControlCreator) {
		this.mapControlCreator = mapControlCreator;
	}

}
