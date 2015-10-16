package org.argeo.eclipse.ui;

import org.eclipse.jface.viewers.ColumnLabelProvider;

/**
 * Wraps the definition of a column to be used in the various JFace viewers
 * (typically tree and table). It enables definition of generic viewers which
 * column can be then defined externally. Also used to generate export.
 */
public class ColumnDefinition {
	private ColumnLabelProvider labelProvider;
	private String label;
	private int weight = 0;
	private int minWidth = 120;

	public ColumnDefinition(ColumnLabelProvider labelProvider, String label) {
		this.labelProvider = labelProvider;
		this.label = label;
	}

	public ColumnDefinition(ColumnLabelProvider labelProvider, String label,
			int weight) {
		this.labelProvider = labelProvider;
		this.label = label;
		this.weight = weight;
		this.minWidth = weight;
	}

	public ColumnDefinition(ColumnLabelProvider labelProvider, String label,
			int weight, int minimumWidth) {
		this.labelProvider = labelProvider;
		this.label = label;
		this.weight = weight;
		this.minWidth = minimumWidth;
	}

	public ColumnLabelProvider getLabelProvider() {
		return labelProvider;
	}

	public void setLabelProvider(ColumnLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public int getMinWidth() {
		return minWidth;
	}

	public void setMinWidth(int minWidth) {
		this.minWidth = minWidth;
	}
}