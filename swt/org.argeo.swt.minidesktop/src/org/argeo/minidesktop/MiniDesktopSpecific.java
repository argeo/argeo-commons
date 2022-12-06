package org.argeo.minidesktop;

import java.util.Objects;

import org.eclipse.swt.browser.Browser;

/**
 * This customiser is available to all components, in order to be extended with
 * low-level specific capabilities, which depend on the context (typically
 * differences between RAP and RCP). It does nothing by default.
 */
public class MiniDesktopSpecific {
	protected void addBrowserTitleListener(MiniBrowser miniBrowser, Browser browser) {
	}

	protected void addBrowserOpenWindowListener(MiniBrowser miniBrowser, Browser browser) {
	}

	private static MiniDesktopSpecific SINGLETON = new MiniDesktopSpecific();

	public static void setMiniDesktopSpecific(MiniDesktopSpecific miniDesktopSpecific) {
		Objects.requireNonNull(miniDesktopSpecific);
		SINGLETON = miniDesktopSpecific;
	}

	static MiniDesktopSpecific getMiniDesktopSpecific() {
		return SINGLETON;
	}
}
