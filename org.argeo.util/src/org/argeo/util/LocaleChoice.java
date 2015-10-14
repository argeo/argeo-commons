/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.security.auth.callback.LanguageCallback;

/** Choose in a list of locales. TODO: replace with {@link LanguageCallback} */
public class LocaleChoice {
	private List<Locale> availableLocales = new ArrayList<Locale>();

	private Integer selectedIndex = null;
	private Integer defaultIndex = null;

	// public LocaleCallback(Integer defaultIndex, List<Locale>
	// availableLocales) {
	// this.availableLocales = Collections
	// .unmodifiableList(new ArrayList<Locale>(availableLocales));
	// this.defaultIndex = defaultIndex;
	// this.selectedIndex = defaultIndex;
	// }

	/**
	 * Convenience constructor based on a comma separated list of iso codes (en,
	 * en_US, fr_CA, etc.). Default selection is default locale.
	 */
	public LocaleChoice(String locales, Locale defaultLocale) {
		if (locales == null || locales.trim().equals(""))
			return;
		String[] codes = locales.split(",");
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
			if (locale.equals(defaultLocale))
				defaultIndex = i;
		}

		if (defaultIndex == null)
			defaultIndex = 0;

		this.selectedIndex = defaultIndex;
	}

	public String[] getSupportedLocalesLabels() {
		String[] labels = new String[availableLocales.size()];
		for (int i = 0; i < availableLocales.size(); i++) {
			Locale locale = availableLocales.get(i);
			if (locale.getCountry().equals(""))
				labels[i] = locale.getDisplayLanguage(locale) + " ["
						+ locale.getLanguage() + "]";
			else
				labels[i] = locale.getDisplayLanguage(locale) + " ("
						+ locale.getDisplayCountry(locale) + ") ["
						+ locale.getLanguage() + "_" + locale.getCountry()
						+ "]";

		}
		return labels;
	}

	public Locale getSelectedLocale() {
		if (selectedIndex == null)
			return null;
		return availableLocales.get(selectedIndex);
	}

	public void setSelectedIndex(Integer selectedIndex) {
		this.selectedIndex = selectedIndex;
	}

	public Integer getDefaultIndex() {
		return defaultIndex;
	}

	public List<Locale> getAvailableLocales() {
		return availableLocales;
	}

	public Locale getDefaultLocale() {
		return availableLocales.get(getDefaultIndex());
	}

	public static void main(String[] args) {
		for (String isoL : Locale.getISOLanguages()) {
			Locale locale = new Locale(isoL);
			System.out.println(isoL + "\t" + locale.getDisplayLanguage() + "\t"
					+ locale.getDisplayLanguage(locale));
		}
	}

}
