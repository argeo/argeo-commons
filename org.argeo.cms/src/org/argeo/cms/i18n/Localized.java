package org.argeo.cms.i18n;

import java.util.Locale;

/** Localized object. */
public interface Localized {
	/** Default assumes that this is an {@link Enum} */
	default Object local(Locale locale) {
		return LocaleUtils.local((Enum<?>) this, locale);
	}

	default String lead() {
		return LocaleUtils.lead(this);
	}

	default String lead(Locale locale) {
		return LocaleUtils.lead(local(locale).toString(), locale);
	}

}
