package org.argeo.jcr;

import javax.jcr.RepositoryException;

/**
 * Wraps a {@link RepositoryException} in a {@link RuntimeException}.
 */
public class JcrException extends IllegalStateException {
	private static final long serialVersionUID = -4530350094877964989L;

	public JcrException(String message, RepositoryException e) {
		super(message, e);
	}

	public JcrException(RepositoryException e) {
		super(e);
	}

	public RepositoryException getRepositoryCause() {
		return (RepositoryException) getCause();
	}
}
