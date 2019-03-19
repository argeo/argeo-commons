package org.argeo.cms.ui.workbench.rap;

import java.security.PrivilegedAction;
import java.util.Locale;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.argeo.cms.CmsMsg;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.util.CmsUtils;
import org.argeo.cms.util.LoginEntryPoint;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class RapWorkbenchLogin extends LoginEntryPoint {
	// private final static Log log =
	// LogFactory.getLog(RapWorkbenchLogin.class);

	/** Override to provide an application specific workbench advisor */
	protected RapWorkbenchAdvisor createRapWorkbenchAdvisor(String username) {
		return new RapWorkbenchAdvisor(username);
	}

	@Override
	public int createUI() {
		JavaScriptExecutor jsExecutor = RWT.getClient().getService(JavaScriptExecutor.class);
		int returnCode;
		try {
			returnCode = super.createUI();
		} finally {
			// always reload
			// TODO optimise?
			jsExecutor.execute("location.reload()");
		}
		return returnCode;
	}

	@Override
	protected int postLogin() {
		Subject subject = getSubject();
		final Display display = Display.getCurrent();
		if (subject.getPrincipals(X500Principal.class).isEmpty()) {
			RWT.getClient().getService(JavaScriptExecutor.class).execute("location.reload()");
		}
		//
		// RUN THE WORKBENCH
		//
		Integer returnCode = null;
		try {
			returnCode = Subject.doAs(subject, new PrivilegedAction<Integer>() {
				public Integer run() {
					int result = createAndRunWorkbench(display, CurrentUser.getUsername(subject));
					return new Integer(result);
				}
			});
			// explicit workbench closing
			logout();
		} finally {
			display.dispose();
		}
		return returnCode;
	}

	protected int createAndRunWorkbench(Display display, String username) {
		RapWorkbenchAdvisor workbenchAdvisor = createRapWorkbenchAdvisor(username);
		return PlatformUI.createAndRunWorkbench(display, workbenchAdvisor);
	}

	@Override
	protected void extendsCredentialsBlock(Composite credentialsBlock, Locale selectedLocale,
			SelectionListener loginSelectionListener) {
//		Button loginButton = new Button(credentialsBlock, SWT.PUSH);
//		loginButton.setText(CmsMsg.login.lead(selectedLocale));
//		loginButton.setLayoutData(CmsUtils.fillWidth());
//		loginButton.addSelectionListener(loginSelectionListener);
	}

	@Override
	protected Display createDisplay() {
		return PlatformUI.createDisplay();
	}

}
