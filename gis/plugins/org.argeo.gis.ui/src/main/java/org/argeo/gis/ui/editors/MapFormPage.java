package org.argeo.gis.ui.editors;

import javax.jcr.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.gis.ui.MapControlCreator;
import org.argeo.gis.ui.MapViewer;
import org.argeo.gis.ui.MapViewerListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;

/** A form editor page to edit geographical data. */
public class MapFormPage extends FormPage {
	private final static Log log = LogFactory.getLog(MapFormPage.class);

	private Node context;
	private MapViewer mapViewer;
	private MapControlCreator mapControlCreator;

	public MapFormPage(FormEditor editor, String id, String title,
			Node context, MapControlCreator mapControlCreator) {
		super(editor, id, title);
		this.context = context;
		this.mapControlCreator = mapControlCreator;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		Composite parent = managedForm.getForm().getBody();
		parent.setLayout(new FillLayout());

		FormToolkit tk = managedForm.getToolkit();

		Composite mapArea = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		mapArea.setLayout(layout);
		mapViewer = mapControlCreator.createMapControl(context, mapArea);
		mapViewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		// form part
		MapFormPart mapFormPart = new MapFormPart();
		getManagedForm().addPart(mapFormPart);
		mapViewer.addMapViewerListener(mapFormPart);

		tk.adapt(mapViewer.getControl());
	}

	public void setFocus() {
		super.setFocus();
		mapViewer.getControl().setFocus();
	}

	public MapViewer getMapViewer() {
		return mapViewer;
	}

	private static class MapFormPart extends AbstractFormPart implements
			MapViewerListener {

		public void featureSelected(String layerId, String featureId) {
			if (log.isDebugEnabled())
				log.debug("Selected feature '" + featureId + "' of layer '"
						+ layerId + "'");
			markDirty();
		}

		public void featureUnselected(String layerId, String featureId) {
			if (log.isDebugEnabled())
				log.debug("Unselected feature '" + featureId + "' of layer '"
						+ layerId + "'");

			markDirty();
		}

	}
}
