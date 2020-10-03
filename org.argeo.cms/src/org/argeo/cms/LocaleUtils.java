package org.argeo.cms;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.security.auth.Subject;

import org.argeo.cms.auth.CurrentUser;

/** Utilities simplifying the development of localization enums. */
public class LocaleUtils {
	public static String local(Enum<?> en) {
		return local(en, getCurrentLocale(), "/OSGI-INF/l10n/bundle");
	}

	public static String local(Enum<?> en, Locale locale) {
		return local(en, locale, "/OSGI-INF/l10n/bundle");
	}

	public static String local(Enum<?> en, Locale locale, String resource) {
		return local(en, locale, resource, en.getClass().getClassLoader());
	}

	public static String local(Enum<?> en, Locale locale, String resource, ClassLoader classLoader) {
		return local(en.name(), locale, resource, classLoader);
	}

	public static String local(String key, ClassLoader classLoader) {
		return local(key, getCurrentLocale(), "/OSGI-INF/l10n/bundle", classLoader);
	}

	public static String local(String key, Locale locale, ClassLoader classLoader) {
		return local(key, locale, "/OSGI-INF/l10n/bundle", classLoader);
	}

	public static String local(String key, Locale locale, String resource, ClassLoader classLoader) {
		ResourceBundle rb = ResourceBundle.getBundle(resource, locale, classLoader);
		assert key.length() > 2;
		if (isLocaleKey(key))
			key = key.substring(1);
		return rb.getString(key);
	}

	public static boolean isLocaleKey(String str) {
		if (str.length() > 2 && ('%' == str.charAt(0)))
			return true;
		else
			return false;
	}

	public static String lead(String raw, Locale locale) {
		return raw.substring(0, 1).toUpperCase(locale) + raw.substring(1);
	}

	public static String lead(Localized localized) {
		return lead(localized, getCurrentLocale());
	}

	public static String lead(Localized localized, Locale locale) {
		return lead(localized.local(locale).toString(), locale);
	}

	static Locale getCurrentLocale() {
		if (Subject.getSubject(AccessController.getContext()) != null)
			return CurrentUser.locale();
		else
			return Locale.getDefault();
		// return UiContext.getLocale();
		// FIXME look into Subject or settings
		// return Locale.getDefault();
	}

	/** Returns null if argument is null. */
	public static List<Locale> asLocaleList(Object locales) {
		if (locales == null)
			return null;
		ArrayList<Locale> availableLocales = new ArrayList<Locale>();
		String[] codes = locales.toString().split(",");
		for (int i = 0; i < codes.length; i++) {
			String code = codes[i];
			// variant not supported
			int indexUnd = code.indexOf("_");
			Locale locale;
			if (indexUnd > 0) {
				String language = code.substring(0, indexUnd);
				String country = code.substring(indexUnd + 1);
				locale = new Locale(language, country);
			} else {
				locale = new Locale(code);
			}
			availableLocales.add(locale);
		}
		return availableLocales;
	}
}
