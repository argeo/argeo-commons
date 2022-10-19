package org.argeo.cms.ux.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractGuidedForm implements GuidedForm {
	private String formTitle;
	private List<Page> pages = new ArrayList<>();
	private View view;

	@Override
	public abstract void addPages();

	public void addPage(AbstractGuidedFormPage page) {
		page.setView(view);
		pages.add(page);
	}

	@Override
	public boolean canFinish() {
		return false;
	}

	@Override
	public boolean performFinish() {
		return false;
	}

	@Override
	public boolean performCancel() {
		return false;
	}

	@Override
	public int getPageCount() {
		return pages.size();
	}

	@Override
	public List<Page> getPages() {
		return Collections.unmodifiableList(pages);
	}

	@Override
	public Page getStartingPage() {
		if (pages.isEmpty())
			throw new IllegalStateException("No page available");
		return pages.get(0);
	}

	@Override
	public Page getPreviousPage(Page page) {
		int index = pages.indexOf(page);
		if (index == 0 || index == -1) {
			// first page or page not found
			return null;
		}
		return pages.get(index - 1);
	}

	@Override
	public Page getNextPage(Page page) {
		int index = pages.indexOf(page);
		if (index == pages.size() - 1 || index == -1) {
			// last page or page not found
			return null;
		}
		return pages.get(index + 1);
	}

	public void setFormTitle(String formTitle) {
		this.formTitle = formTitle;
	}

	@Override
	public String getFormTitle() {
		return formTitle;
	}

	@Override
	public void setView(View view) {
		this.view = view;
	}

}
