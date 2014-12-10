package org.argeo.eclipse.ui.jcr.lists;

/**
 * Utility object to manage column in various tables and extracts displaying
 * data from JCR
 */
public class ColumnDefinition {
	private final static int DEFAULT_COLUMN_SIZE = 120;

	private String selectorName;
	private String propertyName;
	private String headerLabel;
	private int propertyType;
	private int columnSize = DEFAULT_COLUMN_SIZE;

	/**
	 * new column using default width
	 * 
	 * @param selectorName
	 * @param propertyName
	 * @param propertyType
	 * @param headerLabel
	 */
	public ColumnDefinition(String selectorName, String propertyName,
			int propertyType, String headerLabel) {
		this.selectorName = selectorName;
		this.propertyName = propertyName;
		this.propertyType = propertyType;
		this.headerLabel = headerLabel;
	}

	/**
	 * 
	 * @param selectorName
	 * @param propertyName
	 * @param propertyType
	 * @param headerLabel
	 * @param columnSize
	 */
	public ColumnDefinition(String selectorName, String propertyName,
			int propertyType, String headerLabel, int columnSize) {
		this.selectorName = selectorName;
		this.propertyName = propertyName;
		this.propertyType = propertyType;
		this.headerLabel = headerLabel;
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

	public String getHeaderLabel() {
		return headerLabel;
	}

	public void setHeaderLabel(String headerLabel) {
		this.headerLabel = headerLabel;
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
}