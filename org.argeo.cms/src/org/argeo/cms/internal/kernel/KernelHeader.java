package org.argeo.cms.internal.kernel;

import java.util.List;
import java.util.Locale;

public interface KernelHeader {
	public Locale getDefaultLocale();

	public List<Locale> getLocales();
}
