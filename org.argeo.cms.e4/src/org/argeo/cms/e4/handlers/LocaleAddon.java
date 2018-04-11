package org.argeo.cms.e4.handlers;

import java.security.AccessController;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.security.auth.Subject;

import org.eclipse.e4.core.services.nls.ILocaleChangeService;

public class LocaleAddon {
	@PostConstruct
	public void init(ILocaleChangeService localeChangeService) {
		Subject subject = Subject.getSubject(AccessController.getContext());
		Locale locale = subject.getPublicCredentials(Locale.class).iterator().next();
		localeChangeService.changeApplicationLocale(locale);
	}
}
