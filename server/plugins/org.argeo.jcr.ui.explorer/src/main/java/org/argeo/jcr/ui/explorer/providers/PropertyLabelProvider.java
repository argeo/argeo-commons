package org.argeo.jcr.ui.explorer.providers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.ui.explorer.JcrExplorerConstants;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

public class PropertyLabelProvider extends ColumnLabelProvider implements
		JcrExplorerConstants {

	// To be able to change column order easily
	public static final int COLUMN_PROPERTY = 0;
	public static final int COLUMN_VALUE = 1;
	public static final int COLUMN_ATTRIBUTES = 2;

	// Utils
	protected DateFormat timeFormatter = new SimpleDateFormat(DATE_TIME_FORMAT);

	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		cell.setText(getColumnText(element, cell.getColumnIndex()));
		// Image image = getImage(element);
		// cell.setImage(image);
		// cell.setBackground(getBackground(element));
		// cell.setForeground(getForeground(element));
		// cell.setFont(getFont(element));
	}

	public String getColumnText(Object element, int columnIndex) {
		try {
			if (element instanceof Property) {
				Property prop = (Property) element;
				if (prop.isMultiple()) {
					switch (columnIndex) {
					case COLUMN_PROPERTY:
						return prop.getName();
					case COLUMN_VALUE:
						// Corresponding values are listed on children
						return "";
					case COLUMN_ATTRIBUTES:
						return JcrUtils.getPropertyDefinitionAsString(prop);
					}
				} else {
					switch (columnIndex) {
					case COLUMN_PROPERTY:
						return prop.getName();
					case COLUMN_VALUE:
						return formatValueAsString(prop.getValue());
					case COLUMN_ATTRIBUTES:
						return JcrUtils.getPropertyDefinitionAsString(prop);
					}
				}
			} else if (element instanceof Value) {
				Value val = (Value) element;

				switch (columnIndex) {
				case COLUMN_PROPERTY:
					// Nothing to show
					return "";
				case COLUMN_VALUE:
					return formatValueAsString(val);
				case COLUMN_ATTRIBUTES:
					// Corresponding attributes are listed on the parent
					return "";
				}
			}

		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexepected error while getting property values", re);
		}
		return null;
	}

	private String formatValueAsString(Value value) {
		// TODO enhance this method
		try {
			String strValue;

			if (value.getType() == PropertyType.BINARY)
				strValue = "<binary>";
			else if (value.getType() == PropertyType.DATE)
				strValue = timeFormatter.format(value.getDate().getTime());
			else
				strValue = value.getString();
			return strValue;
		} catch (RepositoryException e) {
			throw new ArgeoException("unexpected error while formatting value",
					e);
		}
	}
}
