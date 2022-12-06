package org.argeo.cms.jface.dialog;

import java.lang.reflect.InvocationTargetException;

import org.argeo.cms.CmsMsg;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.swt.Selected;
import org.argeo.cms.swt.dialogs.LightweightDialog;
import org.argeo.cms.ux.widgets.CmsDialog;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer2;
import org.eclipse.jface.wizard.IWizardPage;
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
public class CmsWizardDialog extends LightweightDialog implements IWizardContainer2 {
	private static final long serialVersionUID = -2123153353654812154L;

	private IWizard wizard;
	private IWizardPage currentPage;
	private int currentPageIndex;

	private Label titleBar;
	private Label message;
	private Composite[] pageBodies;
	private Composite buttons;
	private Button back;
	private Button next;
	private Button finish;

	public CmsWizardDialog(Shell parentShell, IWizard wizard) {
		super(parentShell);
		this.wizard = wizard;
		wizard.setContainer(this);
		// create the pages
		wizard.addPages();
		currentPage = wizard.getStartingPage();
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
			cancelButton.addSelectionListener((Selected) (e) -> closeShell(CmsDialog.CANCEL));
			message = new Label(messageArea, SWT.WRAP);
			message.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 2));
			updateMessage();
		}

		Composite body = new Composite(parent, SWT.BORDER);
		body.setLayout(new FormLayout());
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		pageBodies = new Composite[wizard.getPageCount()];
		IWizardPage[] pages = wizard.getPages();
		for (int i = 0; i < pages.length; i++) {
			pageBodies[i] = new Composite(body, SWT.NONE);
			pageBodies[i].setLayout(CmsSwtUtils.noSpaceGridLayout());
			setSwitchingFormData(pageBodies[i]);
			pages[i].createControl(pageBodies[i]);
		}
		showPage(currentPage);

		buttons = new Composite(parent, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
		{
			boolean singlePage = wizard.getPageCount() == 1;
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

	@Override
	public IWizardPage getCurrentPage() {
		return currentPage;
	}

	@Override
	public Shell getShell() {
		return getForegoundShell();
	}

	@Override
	public void showPage(IWizardPage page) {
		IWizardPage[] pages = wizard.getPages();
		int index = -1;
		for (int i = 0; i < pages.length; i++) {
			if (page == pages[i]) {
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
			back.setEnabled(wizard.getPreviousPage(currentPage) != null);
		if (next != null)
			next.setEnabled(wizard.getNextPage(currentPage) != null && currentPage.canFlipToNextPage());
		if (finish != null) {
			finish.setEnabled(wizard.canFinish());
		}
	}

	@Override
	public void updateMessage() {
		if (currentPage.getMessage() != null)
			message.setText(currentPage.getMessage());
	}

	@Override
	public void updateTitleBar() {
		if (currentPage.getTitle() != null)
			titleBar.setText(currentPage.getTitle());
	}

	@Override
	public void updateWindowTitle() {
		setTitle(wizard.getWindowTitle());
	}

	@Override
	public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
			throws InvocationTargetException, InterruptedException {
		// FIXME it creates a dependency to Eclipse Core Runtime
		// runnable.run(null);
	}

	@Override
	public void updateSize() {
		// TODO pack?
	}

	protected boolean onCancel() {
		return wizard.performCancel();
	}

	protected void nextPressed() {
		IWizardPage page = wizard.getNextPage(currentPage);
		showPage(page);
		updateButtons();
	}

	protected void backPressed() {
		IWizardPage page = wizard.getPreviousPage(currentPage);
		showPage(page);
		updateButtons();
	}

	protected void finishPressed() {
		if (wizard.performFinish())
			closeShell(CmsDialog.OK);
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
