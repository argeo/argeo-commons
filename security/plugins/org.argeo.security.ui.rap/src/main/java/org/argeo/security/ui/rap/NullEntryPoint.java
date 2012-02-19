package org.argeo.security.ui.rap;

import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.ui.PlatformUI;

/**
 * RAP entry point which does doesing except creating the display
 */
public class NullEntryPoint implements IEntryPoint {
	@Override
	public int createUI() {
		// create display
		PlatformUI.createDisplay();
		return 0;
	}
}
