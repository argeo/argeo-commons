package org.argeo.jcr;

import java.util.function.Function;

import javax.jcr.Session;

/** An arbitrary execution on a JCR session, optionally returning a result. */
@FunctionalInterface
public interface JcrCallback extends Function<Session, Object> {
	/** @deprecated Use {@link #apply(Session)} instead. */
	@Deprecated
	public default Object execute(Session session) {
		return apply(session);
	}
}
