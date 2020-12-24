package org.argeo.cms;

import java.text.MessageFormat;
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

	default String format(Object[] args) {
		Locale locale = LocaleUtils.getCurrentLocale();
		MessageFormat format = new MessageFormat(local(locale).toString(), locale);
		return format.format(args);
	}

	default String lead(Locale locale) {
		return LocaleUtils.lead(local(locale).toString(), locale);
	}

	static class Untranslated implements Localized {
		private String msg;

		public Untranslated(String msg) {
			super();
			this.msg = msg;
		}

		@Override
		public Object local(Locale locale) {
			return msg;
		}

	}
}
