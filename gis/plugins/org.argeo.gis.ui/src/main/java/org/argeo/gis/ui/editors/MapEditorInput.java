package org.argeo.gis.ui.editors;

import org.argeo.gis.ui.MapContextProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.geotools.map.MapContext;

public class MapEditorInput implements IEditorInput, MapContextProvider {
	private final MapContext mapContext;

	public MapEditorInput(MapContext mapContext) {
		this.mapContext = mapContext;
	}

	public MapContext getMapContext() {
		return mapContext;
	}

	public String getName() {
		return mapContext.getTitle() != null ? mapContext.getTitle() : "<new>";
	}

	public String getToolTipText() {
		return mapContext.getAbstract() != null ? mapContext.getAbstract()
				: mapContext.getTitle() != null ? mapContext.getTitle() : "";
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		if (MapContext.class.isAssignableFrom(adapter))
			return mapContext;
		return null;
	}

	public boolean exists() {
		return false;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public IPersistableElement getPersistable() {
		return null;
	}

}
