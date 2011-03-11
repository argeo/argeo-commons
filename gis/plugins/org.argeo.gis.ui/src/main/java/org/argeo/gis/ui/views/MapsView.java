package org.argeo.gis.ui.views;

import javax.jcr.Node;
import javax.jcr.Session;

import org.argeo.eclipse.ui.jcr.SimpleNodeContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class MapsView extends ViewPart implements IDoubleClickListener {
	private String mapsBasePath = "/gis/maps";

	private Session session;

	private TreeViewer viewer;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		String[] basePaths = { mapsBasePath };
		viewer.setContentProvider(new SimpleNodeContentProvider(session,
				basePaths));
		viewer.setLabelProvider(new MapsLabelProvider());
		viewer.setInput(getViewSite());
		viewer.addDoubleClickListener(this);
	}

	public void doubleClick(DoubleClickEvent event) {
		if (!event.getSelection().isEmpty()) {
			Object obj = ((IStructuredSelection) event.getSelection())
					.getFirstElement();
			if (obj instanceof Node) {
				//Node node = (Node) obj;
			}

		}

	}

	@Override
	public void setFocus() {
		viewer.getTree().setFocus();
	}

	public void refresh() {
		viewer.refresh();
	}

	private class MapsLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			return super.getText(element);
		}

	}
}
