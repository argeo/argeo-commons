package org.argeo.cms.e4.jcr;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.jcr.Node;

import org.argeo.cms.ui.jcr.model.SingleJcrNodeElem;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

public class JcrNodeEditor {
	final static String DESCRIPTOR_ID = "org.argeo.cms.e4.partdescriptor.nodeEditor";

	@PostConstruct
	public void createUi(Composite parent, MPart part, ESelectionService selectionService) {
		parent.setLayout(new FillLayout());
		List<?> selection = (List<?>) selectionService.getSelection();
		Node node = ((SingleJcrNodeElem) selection.get(0)).getNode();
		GenericPropertyPage propertyPage = new GenericPropertyPage(node);
		propertyPage.createFormContent(parent);
	}

}
