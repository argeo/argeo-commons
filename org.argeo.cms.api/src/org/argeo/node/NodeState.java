package org.argeo.node;

import java.util.List;
import java.util.Locale;

public interface NodeState {
	public Locale getDefaultLocale();

	public List<Locale> getLocales();
}
