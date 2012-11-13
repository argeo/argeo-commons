package org.argeo.util;

import java.util.Locale;

import org.argeo.ArgeoException;

/** Utilities around internationalization. */
public class LocaleUtils {
	/**
	 * The locale of the current thread and its children. Allows to deal with
	 * internationalisation as a cross cutting aspect. Never null.
	 */
	public final static InheritableThreadLocal<Locale> threadLocale = new InheritableThreadLocal<Locale>() {
		@Override
		protected Locale initialValue() {
			return Locale.getDefault();
		}

		@Override
		public void set(Locale value) {
			if (value == null)
				throw new ArgeoException("Thread local cannot be null.");
			super.set(value);
		}

	};
}
