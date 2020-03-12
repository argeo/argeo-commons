package org.argeo.cms.ui.internal.rwt;

import org.argeo.cms.ui.util.LoginEntryPoint;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.Application.OperationMode;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;

public class UserUi implements ApplicationConfiguration {
	@Override
	public void configure(Application application) {
		application.setOperationMode(OperationMode.SWT_COMPATIBILITY);
		application.addEntryPoint("/login", LoginEntryPoint.class, null);
	}
}
