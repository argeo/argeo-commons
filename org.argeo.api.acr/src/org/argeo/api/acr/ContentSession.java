package org.argeo.api.acr;

import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.security.auth.Subject;
import javax.xml.namespace.NamespaceContext;

import org.argeo.api.acr.search.BasicSearch;

/** An authenticated session to a repository. */
public interface ContentSession extends NamespaceContext {
	Subject getSubject();

	Locale getLocale();

	Content get(String path);

	boolean exists(String path);

	CompletionStage<ContentSession> edit(Consumer<ContentSession> work);

	Stream<Content> search(Consumer<BasicSearch> search);
}
