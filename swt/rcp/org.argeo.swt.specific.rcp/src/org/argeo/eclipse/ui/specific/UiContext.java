package org.argeo.eclipse.ui.specific;

import java.util.Locale;

import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

/** Singleton class providing single sources infos about the UI context. */
public class UiContext {

	@Deprecated
	public static RemoteAuthRequest getRemoteAuthRequest() {
		return null;
	}

	@Deprecated
	public static RemoteAuthResponse getRemoteAuthResponse() {
		return null;
	}

	public static Locale getLocale() {
		return Locale.getDefault();
	}

	public static void setLocale(Locale locale) {
		Locale.setDefault(locale);
	}

	/** Can always be null */
	@SuppressWarnings("unchecked")
	public static <T> T getData(String key) {
		Display display = getDisplay();
		if (display == null)
			return null;
		return (T) display.getData(key);
	}

	public static void setData(String key, Object value) {
		Display display = getDisplay();
		if (display == null)
			throw new IllegalStateException("Not display available");
		display.setData(key, value);
	}

	public static void killDisplay(Display display) {
		display.dispose();
	}

	private static Display getDisplay() {
		return Display.getCurrent();
	}

	/** The zoom level in percent of the monitor where this control is located. */
	public static int getZoom(Control control) {
		Shell shell = control.getShell();
		Monitor[] monitors = shell.getDisplay().getMonitors();

		Rectangle shellBounds = shell.getBounds();
		Monitor monitor = null;
		int currentArea = 0;
		for (int i = 0; i < monitors.length; i++) {
			Rectangle monitorBounds = monitors[i].getBounds();
			if (monitorBounds.intersects(shellBounds)) {
				Rectangle intersection = monitorBounds.intersection(shellBounds);
				int area = intersection.width * intersection.height;
				if (area > currentArea) {
					currentArea = area;
					monitor = monitors[i];
				}
			}
		}
		if (monitor == null) // TODO not clear how it could happen
			throw new IllegalStateException("Cannot find monitor for control " + control);
		return monitor.getZoom();
	}

	private UiContext() {
	}

}
