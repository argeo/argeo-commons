package org.argeo.eclipse.ui.jcr.lists;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.argeo.ArgeoException;
import org.eclipse.jface.viewers.ColumnLabelProvider;

/**
 * Base implementation of a label provider for widgets that display JCR Rows.
 */
public class SimpleJcrNodeLabelProvider extends ColumnLabelProvider {

	private final static String DEFAULT_DATE_FORMAT = "EEE, dd MMM yyyy";
	private final static String DEFAULT_NUMBER_FORMAT = "#,##0.0";

	private DateFormat dateFormat;
	private NumberFormat numberFormat;

	final private String propertyName;

	/**
	 * Default Label provider for a given property of a node. Using default
	 * pattern for date and number formating
	 */
	public SimpleJcrNodeLabelProvider(String propertyName) {
		this.propertyName = propertyName;
		dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
		numberFormat = DecimalFormat.getInstance();
		((DecimalFormat) numberFormat).applyPattern(DEFAULT_NUMBER_FORMAT);
	}

	/**
	 * Label provider for a given property of a node optionally precising date
	 * and/or number format patterns
	 */
	public SimpleJcrNodeLabelProvider(String propertyName,
			String dateFormatPattern, String numberFormatPattern) {
		this.propertyName = propertyName;
		dateFormat = new SimpleDateFormat(
				dateFormatPattern == null ? DEFAULT_DATE_FORMAT
						: dateFormatPattern);
		numberFormat = DecimalFormat.getInstance();
		((DecimalFormat) numberFormat)
				.applyPattern(numberFormatPattern == null ? DEFAULT_NUMBER_FORMAT
						: numberFormatPattern);
	}

	@Override
	public String getText(Object element) {
		try {
			Node currNode = (Node) element;

			Value value = null;
			if (currNode.hasProperty(propertyName))
				value = currNode.getProperty(propertyName).getValue();
			else
				return "";

			switch (value.getType()) {
			case PropertyType.STRING:
				return value.getString();
			case PropertyType.BOOLEAN:
				return "" + value.getBoolean();
			case PropertyType.DATE:
				return dateFormat.format(value.getDate().getTime());
			case PropertyType.LONG:
				return "" + value.getLong();
			case PropertyType.DECIMAL:
				return numberFormat.format(value.getDecimal());
			case PropertyType.DOUBLE:
				return numberFormat.format(value.getDouble());
			default:
				throw new ArgeoException("Unimplemented label provider "
						+ "for property type " + value.getType());
			}
		} catch (RepositoryException re) {
			throw new ArgeoException("Unable to get text from row", re);
		}
	}

	public void setDateFormat(String dateFormatPattern) {
		dateFormat = new SimpleDateFormat(dateFormatPattern);
	}

	public void setNumberFormat(String numberFormatPattern) {
		((DecimalFormat) numberFormat).applyPattern(numberFormatPattern);
	}
}