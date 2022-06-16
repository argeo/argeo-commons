package org.argeo.cms.ui;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.api.acr.Content;
import org.argeo.cms.jcr.acr.JcrContent;
import org.argeo.cms.swt.acr.SwtUiProvider;
import org.argeo.jcr.JcrException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** Stateless factory building an SWT user interface given a JCR context. */
@FunctionalInterface
public interface CmsUiProvider extends SwtUiProvider {
	/**
	 * Initialises a user interface.
	 * 
	 * @param parent  the parent composite
	 * @param context a context node (holding the JCR underlying session), or null
	 */
	Control createUi(Composite parent, Node context) throws RepositoryException;

	default Control createUiPart(Composite parent, Node context) {
		try {
			return createUi(parent, context);
		} catch (RepositoryException e) {
			throw new JcrException("Cannot create UI for context " + context, e);
		}
	}

	@Override
	default Control createUiPart(Composite parent, Content context) {
		if (context == null)
			return createUiPart(parent, (Node) null);
		if (context instanceof JcrContent) {
			Node node = ((JcrContent) context).getJcrNode();
			return createUiPart(parent, node);
		} else {
			throw new IllegalArgumentException("Content " + context + " is not compatible with JCR");
		}
	}

}
