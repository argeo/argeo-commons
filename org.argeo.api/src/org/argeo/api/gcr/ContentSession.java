package org.argeo.api.gcr;

import java.util.Locale;

import javax.security.auth.Subject;

public interface ContentSession {
	Subject getSubject();

	Locale getLocale();
}
