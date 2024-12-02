package org.argeo.eclipse.ui.specific;

/** The supported SWT variants. */
public enum SwtVariant {
	RAP, RCP;

	public boolean isCurrentVariant() {
		return this == RCP;
	}
}
