package org.argeo.eclipse.ui.specific;

import java.util.function.IntSupplier;

import org.eclipse.swt.SWT;

/**
 * Constants available in RAP but not RCP, and vice-versa. Enum is used instead
 * of static constants so that the value does not get inlined.
 */
public enum SwtIntConstant implements IntSupplier {
	EMBEDDED(SWT.EMBEDDED);

	private int value;

	private SwtIntConstant(int value) {
		this.value = value;
	}

	@Override
	public int getAsInt() {
		return value;
	}

}
