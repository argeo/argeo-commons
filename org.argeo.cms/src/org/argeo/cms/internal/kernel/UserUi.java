package org.argeo.cms.internal.kernel;

import org.argeo.cms.util.LoginEntryPoint;
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
