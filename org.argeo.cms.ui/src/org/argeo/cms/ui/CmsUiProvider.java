package org.argeo.cms.ui;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.node.MvcProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** Stateless factory building an SWT user interface given a JCR context. */
@FunctionalInterface
public interface CmsUiProvider extends MvcProvider<Composite, Node, Control> {
	/**
	 * Initialises a user interface.
	 * 
	 * @param parent  the parent composite
	 * @param context a context node (holding the JCR underlying session), or null
	 */
	public Control createUi(Composite parent, Node context) throws RepositoryException;

	@Override
	public default Control apply(Composite parent, Node context) {
		try {
			return createUi(parent, context);
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot create UI for context " + context, e);
		}
	}

}
