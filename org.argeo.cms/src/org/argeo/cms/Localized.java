package org.argeo.cms;

import java.text.MessageFormat;
import java.util.Locale;

/** Localized object. */
@FunctionalInterface
public interface Localized {
	String name();

	default ClassLoader getL10nClassLoader() {
		return getClass().getClassLoader();
	}

	/** Default assumes that this is an {@link Enum} */
	default String local(Locale locale) {
		return LocaleUtils.local(this, locale);
	}

	default String lead() {
		return LocaleUtils.lead(this);
	}

	default String local() {
		return LocaleUtils.local(this);
	}

	default String format(Object[] args) {
		Locale locale = LocaleUtils.getCurrentLocale();
		MessageFormat format = new MessageFormat(local(locale).toString(), locale);
		return format.format(args);
	}

	default String lead(Locale locale) {
		return LocaleUtils.toLead(local(locale).toString(), locale);
	}

	static class Untranslated implements Localized {
		private String msg;

		public Untranslated(String msg) {
			super();
			this.msg = msg;
		}

		@Override
		public String local(Locale locale) {
			return msg;
		}

		@Override
		public String name() {
			return null;
		}

	}
}
