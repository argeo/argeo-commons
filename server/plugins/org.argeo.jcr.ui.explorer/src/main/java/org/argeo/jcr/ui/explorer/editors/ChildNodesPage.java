package org.argeo.jcr.ui.explorer.editors;

import javax.jcr.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jcr.ui.explorer.browser.NodeLabelProvider;
import org.argeo.jcr.ui.explorer.browser.SingleNodeAsTreeContentProvider;
import org.argeo.jcr.ui.explorer.utils.GenericNodeDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * List all childs of the current node and brings some browsing capabilities
 * accross the repository
 */
public class ChildNodesPage extends FormPage {
	private final static Log log = LogFactory.getLog(ChildNodesPage.class);

	// business objects
	private Node currentNode;

	// this page UI components
	private SingleNodeAsTreeContentProvider nodeContentProvider;
	private TreeViewer nodesViewer;

	public ChildNodesPage(FormEditor editor, String title, Node currentNode) {
		super(editor, "ChildNodesPage", title);
		this.currentNode = currentNode;
	}

	protected void createFormContent(IManagedForm managedForm) {
		try {
			ScrolledForm form = managedForm.getForm();
			Composite body = form.getBody();
			GridLayout twt = new GridLayout(1, false);
			twt.marginWidth = twt.marginHeight = 0;
			body.setLayout(twt);

			nodeContentProvider = new SingleNodeAsTreeContentProvider();
			nodesViewer = createNodeViewer(body, nodeContentProvider);
			nodesViewer.setInput(currentNode);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected TreeViewer createNodeViewer(Composite parent,
			final ITreeContentProvider nodeContentProvider) {

		final TreeViewer tmpNodeViewer = new TreeViewer(parent, SWT.MULTI);

		tmpNodeViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		tmpNodeViewer.setContentProvider(nodeContentProvider);
		tmpNodeViewer.setLabelProvider(new NodeLabelProvider());
		tmpNodeViewer
				.addDoubleClickListener(new GenericNodeDoubleClickListener(
						tmpNodeViewer));
		return tmpNodeViewer;
	}
}
