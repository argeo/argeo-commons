package org.argeo.cms.script;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.ui.fm.FmUiProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class Html {
	private String template;
	private Node context;

	private Control control;

	public Html(Composite parent, String template, Node context) throws RepositoryException {
		this.template = template;
		this.context = context;
		this.control = new FmUiProvider(this.template).createUi(parent, context);
	}

	public Html(Composite parent, String template) throws RepositoryException {
		this(parent, template, null);
	}

	public String getTemplate() {
		return template;
	}

	public Node getContext() {
		return context;
	}

	public Control getControl() {
		return control;
	}

}
