package org.argeo.eclipse.ui.jcr.lists;

import javax.jcr.Node;
import javax.jcr.query.Row;

import org.argeo.eclipse.ui.ColumnDefinition;

/**
 * Utility object to manage column in various tables and extracts displaying
 * data from JCR
 */
public class JcrColumnDefinition extends ColumnDefinition {
	private final static int DEFAULT_COLUMN_SIZE = 120;

	private String selectorName;
	private String propertyName;
	private int propertyType;
	private int columnSize;

	/**
	 * Use this kind of columns to configure a table that displays JCR
	 * {@link Row}
	 * 
	 * @param selectorName
	 * @param propertyName
	 * @param propertyType
	 * @param headerLabel
	 */
	public JcrColumnDefinition(String selectorName, String propertyName,
			int propertyType, String headerLabel) {
		super(new SimpleJcrRowLabelProvider(selectorName, propertyName),
				headerLabel);
		this.selectorName = selectorName;
		this.propertyName = propertyName;
		this.propertyType = propertyType;
		this.columnSize = DEFAULT_COLUMN_SIZE;
	}

	/**
	 * Use this kind of columns to configure a table that displays JCR
	 * {@link Row}
	 * 
	 * @param selectorName
	 * @param propertyName
	 * @param propertyType
	 * @param headerLabel
	 * @param columnSize
	 */
	public JcrColumnDefinition(String selectorName, String propertyName,
			int propertyType, String headerLabel, int columnSize) {
		super(new SimpleJcrRowLabelProvider(selectorName, propertyName),
				headerLabel, columnSize);
		this.selectorName = selectorName;
		this.propertyName = propertyName;
		this.propertyType = propertyType;
		this.columnSize = columnSize;
	}

	/**
	 * Use this kind of columns to configure a table that displays JCR
	 * {@link Node}
	 * 
	 * @param propertyName
	 * @param propertyType
	 * @param headerLabel
	 * @param columnSize
	 */
	public JcrColumnDefinition(String propertyName, int propertyType,
			String headerLabel, int columnSize) {
		super(new SimpleJcrNodeLabelProvider(propertyName), headerLabel,
				columnSize);
		this.propertyName = propertyName;
		this.propertyType = propertyType;
		this.columnSize = columnSize;
	}

	public String getSelectorName() {
		return selectorName;
	}

	public void setSelectorName(String selectorName) {
		this.selectorName = selectorName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public int getPropertyType() {
		return propertyType;
	}

	public void setPropertyType(int propertyType) {
		this.propertyType = propertyType;
	}

	public int getColumnSize() {
		return columnSize;
	}

	public void setColumnSize(int columnSize) {
		this.columnSize = columnSize;
	}

	public String getHeaderLabel() {
		return super.getLabel();
	}

	public void setHeaderLabel(String headerLabel) {
		super.setLabel(headerLabel);
	}
}
