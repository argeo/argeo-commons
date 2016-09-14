package org.argeo.cms.maintenance;

import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;

public class MaintenanceUi implements ApplicationConfiguration {

	@Override
	public void configure(Application application) {
		application.addEntryPoint("/status", DeploymentEntryPoint.class, null);
	}

}
