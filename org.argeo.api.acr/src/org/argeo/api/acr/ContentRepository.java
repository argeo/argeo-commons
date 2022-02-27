package org.argeo.api.acr;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * A content repository is an actually running implementation of various kind of
 * content system. It allows a pre-authenticated caller to open a session.
 */
public interface ContentRepository extends Supplier<ContentSession> {
	ContentSession get(Locale locale);
}
