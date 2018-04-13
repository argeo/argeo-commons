package org.argeo.cms.ui.dialogs;

import java.lang.reflect.InvocationTargetException;

import org.argeo.cms.CmsException;
import org.argeo.cms.util.CmsUtils;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.Selected;
import org.argeo.eclipse.ui.dialogs.LightweightDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer2;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class CmsWizardDialog extends LightweightDialog implements IWizardContainer2 {
	private static final long serialVersionUID = -2123153353654812154L;

	private IWizard wizard;
	private IWizardPage currentPage;

	private Label titleBar;
	private Label message;
	private Composite body;
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
			throw new CmsException("At least one wizard page is required");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		updateWindowTitle();

		Composite messageArea = new Composite(parent, SWT.NONE);
		messageArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		{
			messageArea.setLayout(CmsUtils.noSpaceGridLayout(new GridLayout(2, false)));
			titleBar = new Label(messageArea, SWT.WRAP);
			titleBar.setFont(EclipseUiUtils.getBoldFont(parent));
			titleBar.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
			updateTitleBar();
			Button cancelButton = new Button(messageArea, SWT.FLAT);
			cancelButton.setText("Cancel");
			cancelButton.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false, 1, 3));
			cancelButton.addSelectionListener((Selected) (e) -> closeShell(CANCEL));
			message = new Label(messageArea, SWT.WRAP);
			message.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 2));
			updateMessage();
		}

		body = new Composite(parent, SWT.BORDER);
		body.setLayout(CmsUtils.noSpaceGridLayout());
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
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
				back.setText("Back");
				back.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				back.addSelectionListener((Selected) (e) -> backPressed());

				next = new Button(buttons, SWT.PUSH);
				next.setText("Next");
				next.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				next.addSelectionListener((Selected) (e) -> nextPressed());
			}
			finish = new Button(buttons, SWT.PUSH);
			finish.setText("Finish");
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
		// clear
		for (Control c : body.getChildren())
			c.dispose();
		page.createControl(body);
		currentPage = page;
	}

	@Override
	public void updateButtons() {
		if (back != null)
			back.setEnabled(wizard.getPreviousPage(currentPage) != null);
		if (next != null)
			next.setEnabled(wizard.getNextPage(currentPage) != null && currentPage.canFlipToNextPage());
		finish.setEnabled(wizard.canFinish());
	}

	@Override
	public void updateMessage() {
		message.setText(currentPage.getMessage());
	}

	@Override
	public void updateTitleBar() {
		titleBar.setText(currentPage.getTitle());
	}

	@Override
	public void updateWindowTitle() {
		setTitle(wizard.getWindowTitle());
	}

	@Override
	public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
			throws InvocationTargetException, InterruptedException {
		runnable.run(null);
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
	}

	protected void backPressed() {
		IWizardPage page = wizard.getPreviousPage(currentPage);
		showPage(page);
	}

	protected void finishPressed() {
		if (wizard.performFinish())
			closeShell(OK);
	}
}
