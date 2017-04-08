package org.argeo.cms.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/** Utilities simplifying the development of localization enums. */
public class LocaleUtils {
	public static Object local(Enum<?> en) {
		return local(en, getCurrentLocale(), "/OSGI-INF/l10n/bundle");
	}

	public static Object local(Enum<?> en, Locale locale) {
		return local(en, locale, "/OSGI-INF/l10n/bundle");
	}

	public static Object local(Enum<?> en, Locale locale, String resource) {
		return local(en, locale, resource, en.getClass().getClassLoader());
	}

	public static Object local(Enum<?> en, Locale locale, String resource, ClassLoader classLoader) {
		ResourceBundle rb = ResourceBundle.getBundle(resource, locale, classLoader);
		return rb.getString(en.name());
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
		// return UiContext.getLocale();
		// FIXME look into Subject or settings
		return Locale.getDefault();
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
