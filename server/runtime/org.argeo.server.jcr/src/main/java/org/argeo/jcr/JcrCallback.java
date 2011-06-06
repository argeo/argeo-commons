package org.argeo.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/** An arbitrary execution on a JCR session, optionally returning a result. */
public interface JcrCallback {
	public Object execute(Session session);
}
