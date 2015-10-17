package org.argeo.cms.internal.kernel;

import org.argeo.cms.util.LoginEntryPoint;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.application.EntryPointFactory;
import org.eclipse.rap.rwt.application.Application.OperationMode;

public class UserUi implements ApplicationConfiguration {

	@Override
	public void configure(Application application) {
		application.setOperationMode(OperationMode.SWT_COMPATIBILITY);
		application.addEntryPoint("/login", LoginEntryPoint.class, null);
	}

	private class LoginEpf implements EntryPointFactory {

		@Override
		public EntryPoint create() {
			return new LoginEntryPoint();
		}

	}
}
