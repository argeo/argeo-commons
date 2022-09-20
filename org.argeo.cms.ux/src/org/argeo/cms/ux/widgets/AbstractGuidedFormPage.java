package org.argeo.cms.ux.widgets;

import org.argeo.cms.ux.widgets.GuidedForm.View;
import org.argeo.cms.ux.widgets.GuidedForm.Page;

public class AbstractGuidedFormPage implements Page {
	private String pageName;
	private String title;
	private View view;

	public AbstractGuidedFormPage(String pageName) {
		super();
		this.pageName = pageName;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setView(View container) {
		this.view = container;

	}

	public String getPageName() {
		return pageName;
	}

	@Override
	public String getTitle() {
		return title;
	}

	public View getView() {
		return view;
	}

}
