package org.argeo.cms;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** Stateless factory building an SWT user interface given a JCR context. */
public interface CmsUiProvider {
	/**
	 * Initialises a user interface.
	 * 
	 * @param parent
	 *            the parent composite
	 * @param a
	 *            context node or null
	 */
	public Control createUi(Composite parent, Node context)
			throws RepositoryException;
}
