package org.argeo.cms.swt.widgets;

import java.util.List;

import org.argeo.cms.CmsMsg;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.swt.Selected;
import org.argeo.cms.swt.dialogs.LightweightDialog;
import org.argeo.cms.ux.widgets.GuidedForm;
import org.argeo.cms.ux.widgets.GuidedForm.Page;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** A wizard dialog based on {@link LightweightDialog}. */
public class SwtGuidedFormDialog extends LightweightDialog implements GuidedForm.View {
	private GuidedForm guidedForm;
	private GuidedForm.Page currentPage;
	private int currentPageIndex;

	private Label titleBar;
	private Label message;
	private Composite[] pageBodies;
	private Composite buttons;
	private Button back;
	private Button next;
	private Button finish;

	public SwtGuidedFormDialog(Shell parentShell, GuidedForm guidedForm) {
		super(parentShell);
		this.guidedForm = guidedForm;
		guidedForm.setView(this);
		// create the pages
		guidedForm.addPages();
		for (Page page : guidedForm.getPages()) {
			if (!(page instanceof SwtGuidedFormPage))
				throw new IllegalArgumentException("Pages form must implement " + SwtGuidedFormPage.class);
		}
		currentPage = guidedForm.getStartingPage();
		if (currentPage == null)
			throw new IllegalArgumentException("At least one wizard page is required");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		updateWindowTitle();

		Composite messageArea = new Composite(parent, SWT.NONE);
		messageArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		{
			messageArea.setLayout(CmsSwtUtils.noSpaceGridLayout(new GridLayout(2, false)));
			titleBar = new Label(messageArea, SWT.WRAP);
			titleBar.setFont(EclipseUiUtils.getBoldFont(parent));
			titleBar.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
			updateTitleBar();
			Button cancelButton = new Button(messageArea, SWT.FLAT);
			cancelButton.setText(CmsMsg.cancel.lead());
			cancelButton.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false, 1, 3));
			cancelButton.addSelectionListener((Selected) (e) -> closeShell(CANCEL));
			message = new Label(messageArea, SWT.WRAP);
			message.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 2));
			updateMessage();
		}

		Composite body = new Composite(parent, SWT.BORDER);
		body.setLayout(new FormLayout());
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		pageBodies = new Composite[guidedForm.getPageCount()];
		List<GuidedForm.Page> pages = guidedForm.getPages();
		for (int i = 0; i < pages.size(); i++) {
			pageBodies[i] = new Composite(body, SWT.NONE);
			pageBodies[i].setLayout(CmsSwtUtils.noSpaceGridLayout());
			setSwitchingFormData(pageBodies[i]);
			// !! SWT specific
			SwtGuidedFormPage page = (SwtGuidedFormPage) pages.get(i);
			page.createControl(pageBodies[i]);
		}
		showPage(currentPage);

		buttons = new Composite(parent, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
		{
			boolean singlePage = guidedForm.getPageCount() == 1;
			// singlePage = false;// dev
			GridLayout layout = new GridLayout(singlePage ? 1 : 3, true);
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			buttons.setLayout(layout);
			// TODO revert order for right-to-left languages

			if (!singlePage) {
				back = new Button(buttons, SWT.PUSH);
				back.setText(CmsMsg.wizardBack.lead());
				back.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				back.addSelectionListener((Selected) (e) -> backPressed());

				next = new Button(buttons, SWT.PUSH);
				next.setText(CmsMsg.wizardNext.lead());
				next.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				next.addSelectionListener((Selected) (e) -> nextPressed());
			}
			finish = new Button(buttons, SWT.PUSH);
			finish.setText(CmsMsg.wizardFinish.lead());
			finish.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			finish.addSelectionListener((Selected) (e) -> finishPressed());

			updateButtons();
		}
		return body;
	}

	public GuidedForm.Page getCurrentPage() {
		return currentPage;
	}

	public Shell getShell() {
		return getForegoundShell();
	}

	public void showPage(GuidedForm.Page page) {
		List<GuidedForm.Page> pages = guidedForm.getPages();
		int index = -1;
		for (int i = 0; i < pages.size(); i++) {
			if (page == pages.get(i)) {
				index = i;
				break;
			}
		}
		if (index < 0)
			throw new IllegalArgumentException("Cannot find index of wizard page " + page);
		pageBodies[index].moveAbove(pageBodies[currentPageIndex]);

		// // clear
		// for (Control c : body.getChildren())
		// c.dispose();
		// page.createControl(body);
		// body.layout(true, true);
		currentPageIndex = index;
		currentPage = page;
	}

	@Override
	public void updateButtons() {
		if (back != null)
			back.setEnabled(guidedForm.getPreviousPage(currentPage) != null);
		if (next != null)
			next.setEnabled(guidedForm.getNextPage(currentPage) != null && currentPage.canFlipToNextPage());
		if (finish != null) {
			finish.setEnabled(guidedForm.canFinish());
		}
	}

	public void updateMessage() {
		if (currentPage.getMessage() != null)
			message.setText(currentPage.getMessage());
	}

	public void updateTitleBar() {
		if (currentPage.getTitle() != null)
			titleBar.setText(currentPage.getTitle());
	}

	public void updateWindowTitle() {
		setTitle(guidedForm.getFormTitle());
	}

	protected boolean onCancel() {
		return guidedForm.performCancel();
	}

	protected void nextPressed() {
		GuidedForm.Page page = guidedForm.getNextPage(currentPage);
		showPage(page);
		updateButtons();
	}

	protected void backPressed() {
		GuidedForm.Page page = guidedForm.getPreviousPage(currentPage);
		showPage(page);
		updateButtons();
	}

	protected void finishPressed() {
		if (guidedForm.performFinish())
			closeShell(OK);
	}

	private static void setSwitchingFormData(Composite composite) {
		FormData fdLabel = new FormData();
		fdLabel.top = new FormAttachment(0, 0);
		fdLabel.left = new FormAttachment(0, 0);
		fdLabel.right = new FormAttachment(100, 0);
		fdLabel.bottom = new FormAttachment(100, 0);
		composite.setLayoutData(fdLabel);
	}

}
