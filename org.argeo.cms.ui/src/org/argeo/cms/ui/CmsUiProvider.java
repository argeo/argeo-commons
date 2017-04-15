package org.argeo.cms.ui;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** Stateless factory building an SWT user interface given a JCR context. */
@FunctionalInterface
public interface CmsUiProvider {
	/**
	 * Initialises a user interface.
	 * 
	 * @param parent
	 *            the parent composite
	 * @param context
	 *            a context node (holding the JCR underlying session), or null
	 */
	public Control createUi(Composite parent, Node context) throws RepositoryException;
}
