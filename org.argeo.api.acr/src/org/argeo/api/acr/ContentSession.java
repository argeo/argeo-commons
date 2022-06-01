package org.argeo.api.acr;

import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import javax.security.auth.Subject;
import javax.xml.namespace.NamespaceContext;

public interface ContentSession extends NamespaceContext {
	Subject getSubject();

	Locale getLocale();

	Content get(String path);

	CompletionStage<ContentSession> edit(Consumer<ContentSession> work);
}
