package org.argeo.eclipse.ui.specific;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.widgets.Display;

/** Singleton class providing single sources infos about the UI context. */
public class UiContext {
	/** Can be null, thus indicating that we are not in a web context. */
	public static HttpServletRequest getHttpRequest() {
		return RWT.getRequest();
	}

	public static Locale getLocale() {
		if (Display.getCurrent() != null)
			return RWT.getUISession().getLocale();
		else
			return Locale.getDefault();
	}

	public static void setLocale(Locale locale) {
		if (Display.getCurrent() != null)
			RWT.getUISession().setLocale(locale);
		else
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
