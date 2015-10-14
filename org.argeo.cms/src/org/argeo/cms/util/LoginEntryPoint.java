package org.argeo.cms.util;

import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class LoginEntryPoint extends AbstractEntryPoint {
	@Override
	public void createContents(Composite parent) {
		parent.setLayout(new GridLayout());
		// CompositeCallbackHandler cch = new CompositeCallbackHandler(parent,
		// SWT.NONE);
		// UserMenu userMenu = new UserMenu(null);

		// Subject subject = new Subject();
		// try {
		// LoginContext lc = new LoginContext(
		// AuthConstants.LOGIN_CONTEXT_USER, subject, userMenu);
		// lc.login();
		// } catch (LoginException e1) {
		// throw new CmsException("Cannot logint", e1);
		// }
	}

}
