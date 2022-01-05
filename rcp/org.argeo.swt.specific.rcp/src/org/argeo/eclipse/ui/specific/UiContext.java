package org.argeo.eclipse.ui.specific;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.swt.widgets.Display;

/** Singleton class providing single sources infos about the UI context. */
public class UiContext {

	public static HttpServletRequest getHttpRequest() {
		return null;
	}

	public static HttpServletResponse getHttpResponse() {
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

	private static Display getDisplay() {
		return Display.getCurrent();
	}

	private UiContext() {
	}

}
