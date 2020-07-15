package org.argeo.cms.ui.widgets.auth;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.security.auth.callback.LanguageCallback;

import org.argeo.cms.CmsException;
import org.argeo.cms.LocaleUtils;

/** Choose in a list of locales. TODO: replace with {@link LanguageCallback} */
public class LocaleChoice {
	private final List<Locale> locales;

	private Integer selectedIndex = null;
	private final Integer defaultIndex;

	public LocaleChoice(List<Locale> locales, Locale defaultLocale) {
		Integer defaultIndex = null;
		this.locales = Collections.unmodifiableList(locales);
		for (int i = 0; i < locales.size(); i++)
			if (locales.get(i).equals(defaultLocale))
				defaultIndex = i;

		// based on language only
		if (defaultIndex == null)
			for (int i = 0; i < locales.size(); i++)
				if (locales.get(i).getLanguage().equals(defaultLocale.getLanguage()))
					defaultIndex = i;

		if (defaultIndex == null)
			throw new CmsException("Default locale " + defaultLocale + " is not in available locales " + locales);
		this.defaultIndex = defaultIndex;

		this.selectedIndex = defaultIndex;
	}

	/**
	 * Convenience constructor based on a comma separated list of iso codes (en,
	 * en_US, fr_CA, etc.). Default selection is default locale.
	 */
	public LocaleChoice(String locales, Locale defaultLocale) {
		this(LocaleUtils.asLocaleList(locales), defaultLocale);
	}

	public String[] getSupportedLocalesLabels() {
		String[] labels = new String[locales.size()];
		for (int i = 0; i < locales.size(); i++) {
			Locale locale = locales.get(i);
			if (locale.getCountry().equals(""))
				labels[i] = locale.getDisplayLanguage(locale) + " [" + locale.getLanguage() + "]";
			else
				labels[i] = locale.getDisplayLanguage(locale) + " (" + locale.getDisplayCountry(locale) + ") ["
						+ locale.getLanguage() + "_" + locale.getCountry() + "]";

		}
		return labels;
	}

	public Locale getSelectedLocale() {
		if (selectedIndex == null)
			return null;
		return locales.get(selectedIndex);
	}

	public void setSelectedIndex(Integer selectedIndex) {
		this.selectedIndex = selectedIndex;
	}

	public Integer getSelectedIndex() {
		return selectedIndex;
	}

	public Integer getDefaultIndex() {
		return defaultIndex;
	}

	public List<Locale> getLocales() {
		return locales;
	}

	public Locale getDefaultLocale() {
		return locales.get(getDefaultIndex());
	}
}
