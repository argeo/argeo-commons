package org.argeo.cms.ux.widgets;

import java.util.List;

public interface GuidedForm {
	String getFormTitle();

	boolean canFinish();

	boolean performFinish();

	boolean performCancel();

	void addPages();

	int getPageCount();

	List<Page> getPages();

	Page getStartingPage();

	Page getPreviousPage(Page page);

	Page getNextPage(Page page);

	void setView(View view);

	interface Page {

		default boolean canFlipToNextPage() {
			return true;
		}

		default String getMessage() {
			return null;
		}

		String getTitle();

	}

	interface View {
		void updateButtons();
	}
}
