package org.argeo.eclipse.ui.specific;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.widgets.Display;

/** Singleton class providing single sources infos about the UI context. */
public class UiContext {

	public static HttpServletRequest getHttpRequest() {
		return RWT.getRequest();
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
			throw new SingleSourcingException(
					"Not display available in RAP context");
		display.setData(key, value);
	}

	private static Display getDisplay() {
		return Display.getCurrent();
	}

	private UiContext() {
	}

}
