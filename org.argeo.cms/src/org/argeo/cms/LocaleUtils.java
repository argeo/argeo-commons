package org.argeo.cms;

import java.util.Locale;
import java.util.ResourceBundle;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.auth.CurrentUser;

/** Utilities simplifying the development of localization enums. */
public class LocaleUtils {
	final static String DEFAULT_OSGI_l10N_BUNDLE = "/OSGI-INF/l10n/bundle";

	private final static CmsLog log = CmsLog.getLog(LocaleUtils.class);

	private final static ThreadLocal<Locale> threadLocale = new ThreadLocal<>();

	public static void setThreadLocale(Locale locale) {
		threadLocale.set(locale);
	}

	public static String local(Localized localized) {
		return local(localized.name(), localized.getClass().getClassLoader());
	}

	public static String local(Localized localized, Locale locale) {
		if (localized.name() == null) // untranslated
			return localized.local(locale);
		return local(localized.name(), locale, localized.getClass().getClassLoader());
	}

	@Deprecated
	public static String local(Enum<?> en) {
		return local(en, getCurrentLocale(), DEFAULT_OSGI_l10N_BUNDLE);
	}

	@Deprecated
	public static String local(Enum<?> en, Locale locale) {
		return local(en, locale, DEFAULT_OSGI_l10N_BUNDLE);
	}

	@Deprecated
	public static String local(Enum<?> en, Locale locale, String resource) {
		return local(en, locale, resource, en.getClass().getClassLoader());
	}

	@Deprecated
	public static String local(Enum<?> en, Locale locale, String resource, ClassLoader classLoader) {
		return local(en.name(), locale, resource, classLoader);
	}

	public static String local(String key, ClassLoader classLoader) {
		return local(key, getCurrentLocale(), DEFAULT_OSGI_l10N_BUNDLE, classLoader);
	}

	public static String local(String key, Locale locale, ClassLoader classLoader) {
		return local(key, locale, DEFAULT_OSGI_l10N_BUNDLE, classLoader);
	}

	/** Where the search for a message is actually performed. */
	public static String local(String key, Locale locale, String resource, ClassLoader classLoader) {
		ResourceBundle rb = ResourceBundle.getBundle(resource, locale, classLoader);
		assert key.length() > 2;
		if (isLocaleKey(key))
			key = key.substring(1);
		if (rb.containsKey(key))
			return rb.getString(key);
		else // for simple cases, the key will actually be the English word
			return key;
	}

	public static boolean isLocaleKey(String str) {
		if (str.length() > 2 && ('%' == str.charAt(0)))
			return true;
		else
			return false;
	}

	/** Lead transformation on the translated string. */
	public static String toLead(String raw, Locale locale) {
		return raw.substring(0, 1).toUpperCase(locale) + raw.substring(1);
	}

	public static String lead(Localized localized, ClassLoader classLoader) {
		Locale locale = getCurrentLocale();
		if (localized.name() == null)// untranslated
			return toLead(localized.local(locale), locale);
		return toLead(local(localized.name(), getCurrentLocale(), DEFAULT_OSGI_l10N_BUNDLE, classLoader), locale);
	}

	public static String lead(Localized localized) {
		return lead(localized, localized.getL10nClassLoader());
	}

	public static String lead(Localized localized, Locale locale) {
		return toLead(local(localized, locale), locale);
	}

	static Locale getCurrentLocale() {
		Locale currentLocale = null;
		if (CurrentUser.isAvailable())
			currentLocale = CurrentUser.locale();
		else if (threadLocale.get() != null) {
			currentLocale = threadLocale.get();
		}
		if (log.isTraceEnabled())
			log.trace("Thread #" + Thread.currentThread().getId() + " " + Thread.currentThread().getName() + " locale: "
					+ currentLocale);
		if (currentLocale == null)
			throw new IllegalStateException("No locale found");
		return currentLocale;
		// return UiContext.getLocale();
		// FIXME look into Subject or settings
		// return Locale.getDefault();
	}

}
