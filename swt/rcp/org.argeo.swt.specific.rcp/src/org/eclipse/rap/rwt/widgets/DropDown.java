package org.eclipse.rap.rwt.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Widget;

public class DropDown {
	private boolean visible=false;

	public DropDown(Widget parent, int style) {
		// FIXME implement a shell
	}

	public DropDown(Widget parent) {
		this(parent, SWT.NONE);
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isVisible() {
		return visible;
	}
	
	public void setItems( String[] items ) {
		
	}
	
	public void setSelectionIndex( int selection ) {
		
	}
	
}
