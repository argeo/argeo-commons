package org.argeo.cms.e4.handlers;

import java.util.Locale;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.nls.ILocaleChangeService;

public class ChangeLanguage {
	@Execute
	public void execute(ILocaleChangeService localeChangeService) {
		localeChangeService.changeApplicationLocale(Locale.FRENCH);
	}
}
