package org.argeo.api.acr;

import java.util.Locale;

import javax.security.auth.Subject;
import javax.xml.namespace.NamespaceContext;

public interface ContentSession extends NamespaceContext {
	Subject getSubject();

	Locale getLocale();

	Content get(String path);

}
