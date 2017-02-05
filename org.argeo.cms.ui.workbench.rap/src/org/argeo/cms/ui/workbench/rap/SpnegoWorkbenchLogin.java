package org.argeo.cms.ui.workbench.rap;

import java.security.PrivilegedAction;
import java.util.Locale;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

public class SpnegoWorkbenchLogin extends LoginEntryPoint {
	// private final static Log log =
	// LogFactory.getLog(RapWorkbenchLogin.class);

	/** Override to provide an application specific workbench advisor */
	protected RapWorkbenchAdvisor createRapWorkbenchAdvisor(String username) {
		return new RapWorkbenchAdvisor(username);
	}

	@Override
	public int createUI() {
		HttpServletRequest request = RWT.getRequest();
		String authorization = request.getHeader(HEADER_AUTHORIZATION);
		if (authorization == null || !authorization.startsWith("Negotiate")) {
			HttpServletResponse response = RWT.getResponse();
			response.setStatus(401);
			response.setHeader(HEADER_WWW_AUTHENTICATE, "Negotiate");
			response.setDateHeader("Date", System.currentTimeMillis());
			response.setDateHeader("Expires", System.currentTimeMillis() + (24 * 60 * 60 * 1000));
			response.setHeader("Accept-Ranges", "bytes");
			response.setHeader("Connection", "Keep-Alive");
			response.setHeader("Keep-Alive", "timeout=5, max=97");
			// response.setContentType("text/html; charset=UTF-8");
		}

		int returnCode;
		returnCode = super.createUI();
		return returnCode;
	}

	@Override
	protected int postLogin() {
		Subject subject = getLoginContext().getSubject();
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
		Button loginButton = new Button(credentialsBlock, SWT.PUSH);
		loginButton.setText(CmsMsg.login.lead(selectedLocale));
		loginButton.setLayoutData(CmsUtils.fillWidth());
		loginButton.addSelectionListener(loginSelectionListener);
	}

	@Override
	protected Display createDisplay() {
		return PlatformUI.createDisplay();
	}

}
