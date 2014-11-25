package org.argeo.eclipse.ui.jcr.lists;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Row;

import org.argeo.ArgeoException;

/**
 * Base implementation of a label provider for widgets that display JCR Rows.
 */
public class SimpleJcrRowLabelProvider extends SimpleJcrNodeLabelProvider {
	private static final long serialVersionUID = -3414654948197181740L;

	final private String selectorName;

	/**
	 * Default Label provider for a given property of a row. Using default
	 * pattern for date and number formating
	 */
	public SimpleJcrRowLabelProvider(String selectorName, String propertyName) {
		super(propertyName);
		this.selectorName = selectorName;
	}

	/**
	 * Label provider for a given property of a node optionally precising date
	 * and/or number format patterns
	 */
	public SimpleJcrRowLabelProvider(String selectorName, String propertyName,
			String dateFormatPattern, String numberFormatPattern) {
		super(propertyName, dateFormatPattern, numberFormatPattern);
		this.selectorName = selectorName;
	}

	@Override
	public String getText(Object element) {
		try {
			Row currRow = (Row) element;
			Node currNode = currRow.getNode(selectorName);
			return super.getText(currNode);
		} catch (RepositoryException re) {
			throw new ArgeoException("Unable to get Node " + selectorName
					+ " from row " + element, re);
		}
	}
}