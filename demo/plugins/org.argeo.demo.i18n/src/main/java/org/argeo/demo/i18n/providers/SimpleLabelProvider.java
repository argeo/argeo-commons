package org.argeo.demo.i18n.providers;

import org.argeo.demo.i18n.model.Place;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

public class SimpleLabelProvider extends ColumnLabelProvider {
	public String getText(Object element) {
		if (element instanceof Place) {
			Place place = (Place) element;
			return place.getName();
		} else
			return element.toString();
	}

	@Override
	public Image getImage(Object element) {
		return null;
	}
}
