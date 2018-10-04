package org.argeo.cms.util;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.ui.CmsUiProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class VerticalMenu implements CmsUiProvider {
	private List<CmsUiProvider> items = new ArrayList<CmsUiProvider>();

	@Override
	public Control createUi(Composite parent, Node context) throws RepositoryException {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayoutData(new GridData(SWT.LEAD, SWT.TOP, false, false));
//		part.setData(RWT.CUSTOM_VARIANT, custom);
		part.setLayout(CmsUtils.noSpaceGridLayout());
		for (CmsUiProvider uiProvider : items) {
			Control subPart = uiProvider.createUi(part, context);
			subPart.setLayoutData(new GridData(SWT.LEAD, SWT.TOP, false, false));
		}
		return part;
	}

	public void add(CmsUiProvider uiProvider) {
		items.add(uiProvider);
	}

	public List<CmsUiProvider> getItems() {
		return items;
	}

	public void setItems(List<CmsUiProvider> items) {
		this.items = items;
	}

}
