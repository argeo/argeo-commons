package org.argeo.cms.i18n;

import java.util.List;
import java.util.Locale;

import org.argeo.cms.auth.CurrentUser;

/**
 * Utilities simplifying the development of localization enums.
 * 
 * @deprecated Use {@link org.argeo.cms.LocaleUtils}
 */
@Deprecated
public class LocaleUtils {
	public static Object local(Enum<?> en) {
		return org.argeo.cms.LocaleUtils.local(en);
	}

	public static Object local(Enum<?> en, Locale locale) {
		return org.argeo.cms.LocaleUtils.local(en, locale);
	}

	public static Object local(Enum<?> en, Locale locale, String resource) {
		return org.argeo.cms.LocaleUtils.local(en, locale, resource);
	}

	public static Object local(Enum<?> en, Locale locale, String resource, ClassLoader classLoader) {
		return org.argeo.cms.LocaleUtils.local(en, locale, resource, classLoader);
	}

	public static String lead(String raw, Locale locale) {
		return org.argeo.cms.LocaleUtils.lead(raw, locale);
	}

	public static String lead(Localized localized) {
		return org.argeo.cms.LocaleUtils.lead(localized);
	}

	public static String lead(Localized localized, Locale locale) {
		return org.argeo.cms.LocaleUtils.lead(localized, locale);
	}

	static Locale getCurrentLocale() {
		return CurrentUser.locale();
		// return UiContext.getLocale();
		// FIXME look into Subject or settings
		// return Locale.getDefault();
	}

	/** Returns null if argument is null. */
	public static List<Locale> asLocaleList(Object locales) {
		return org.argeo.cms.LocaleUtils.asLocaleList(locales);
	}
}
