package org.argeo.cms.e4.handlers;

import java.security.AccessController;
import java.util.Locale;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.security.auth.Subject;

import org.argeo.eclipse.ui.specific.UiContext;
import org.eclipse.e4.core.services.nls.ILocaleChangeService;

public class LocaleAddon {
	@PostConstruct
	public void init(ILocaleChangeService localeChangeService) {
		Subject subject = Subject.getSubject(AccessController.getContext());
		Set<Locale> locales = subject.getPublicCredentials(Locale.class);
		if (!locales.isEmpty()) {
			Locale locale = locales.iterator().next();
			localeChangeService.changeApplicationLocale(locale);
			UiContext.setLocale(locale);
		}
	}
}
