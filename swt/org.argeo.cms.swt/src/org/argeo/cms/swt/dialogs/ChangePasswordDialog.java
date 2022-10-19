package org.argeo.cms.swt.dialogs;

import java.util.Arrays;
import java.util.concurrent.Callable;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.ux.CmsView;
import org.argeo.cms.CmsMsg;
import org.argeo.cms.CmsUserManager;
import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** Dialog to change a password. */
public class ChangePasswordDialog extends CmsMessageDialog {
	private final static CmsLog log = CmsLog.getLog(ChangePasswordDialog.class);

	private CmsUserManager cmsUserManager;
	private CmsView cmsView;

	private Callable<Integer> doIt;

	public ChangePasswordDialog(Shell parentShell, String message, int kind, CmsUserManager cmsUserManager) {
		super(parentShell, message, kind);
		this.cmsUserManager = cmsUserManager;
		cmsView = CmsSwtUtils.getCmsView(parentShell);
	}

	@Override
	protected Control createInputArea(Composite userSection) {
		addFormLabel(userSection, CmsMsg.currentPassword.lead());
		Text previousPassword = new Text(userSection, SWT.BORDER | SWT.PASSWORD);
		previousPassword.setLayoutData(CmsSwtUtils.fillWidth());
		addFormLabel(userSection, CmsMsg.newPassword.lead());
		Text newPassword = new Text(userSection, SWT.BORDER | SWT.PASSWORD);
		newPassword.setLayoutData(CmsSwtUtils.fillWidth());
		addFormLabel(userSection, CmsMsg.repeatNewPassword.lead());
		Text confirmPassword = new Text(userSection, SWT.BORDER | SWT.PASSWORD);
		confirmPassword.setLayoutData(CmsSwtUtils.fillWidth());

		doIt = () -> {
			if (Arrays.equals(newPassword.getTextChars(), confirmPassword.getTextChars())) {
				try {
					cmsUserManager.changeOwnPassword(previousPassword.getTextChars(), newPassword.getTextChars());
					return OK;
				} catch (Exception e1) {
					log.error("Could not change password", e1);
					cancel();
					CmsMessageDialog.openError(CmsMsg.invalidPassword.lead());
					return CANCEL;
				}
			} else {
				cancel();
				CmsMessageDialog.openError(CmsMsg.repeatNewPassword.lead());
				return CANCEL;
			}
		};

		pack();
		return previousPassword;
	}

	@Override
	protected void okPressed() {
		Integer returnCode = cmsView.doAs(doIt);
		if (returnCode.equals(OK)) {
			super.okPressed();
			CmsMessageDialog.openInformation(CmsMsg.passwordChanged.lead());
		}
	}

	private static Label addFormLabel(Composite parent, String label) {
		Label lbl = new Label(parent, SWT.WRAP);
		lbl.setText(label);
//		CmsUiUtils.style(lbl, SuiteStyle.simpleLabel);
		return lbl;
	}

}
