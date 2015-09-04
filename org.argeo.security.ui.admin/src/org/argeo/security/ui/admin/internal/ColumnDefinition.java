package org.argeo.security.ui.admin.internal;

import org.eclipse.jface.viewers.ColumnLabelProvider;

/** Centralize the colum definition for the various tables of the useradmin UI */
public class ColumnDefinition {
	private ColumnLabelProvider labelProvider;
	private String label;
	private int weight;
	private int minWidth;

	public ColumnDefinition(ColumnLabelProvider labelProvider, String label,
			int weight, int minimumWidth) {
		this.labelProvider = labelProvider;
		this.label = label;
		this.weight = weight;
		this.minWidth = minimumWidth;
	}

	public ColumnDefinition(ColumnLabelProvider labelProvider, String label,
			int weight) {
		this.labelProvider = labelProvider;
		this.label = label;
		this.weight = weight;
		this.minWidth = weight;
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
