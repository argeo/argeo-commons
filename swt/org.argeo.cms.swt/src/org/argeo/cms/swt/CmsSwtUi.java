package org.argeo.cms.swt;

import org.argeo.api.cms.ux.CmsUi;
import org.argeo.api.cms.ux.CmsView;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/** A basic {@link CmsUi}, based on an SWT {@link Composite}. */
public class CmsSwtUi extends Composite implements CmsUi {

	private static final long serialVersionUID = -107939076610406448L;

	private CmsView cmsView;

	public CmsSwtUi(Composite parent, int style) {
		super(parent, style);
		cmsView = CmsSwtUtils.getCmsView(parent);

		setLayout(new GridLayout());
	}

	public CmsView getCmsView() {
		return cmsView;
	}

}