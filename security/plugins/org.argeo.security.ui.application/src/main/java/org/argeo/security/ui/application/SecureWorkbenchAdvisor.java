package org.argeo.security.ui.application;

import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class SecureWorkbenchAdvisor extends WorkbenchAdvisor {

	static final String DEFAULT_PERSPECTIVE_ID = "org.argeo.security.ui.securityPerspective"; //$NON-NLS-1$

	public final static String INITIAL_PERSPECTIVE_PROPERTY = "org.argeo.security.ui.initialPerspective";
	private String initialPerspective = System.getProperty(
			INITIAL_PERSPECTIVE_PROPERTY, DEFAULT_PERSPECTIVE_ID);

	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new SecureWorkbenchWindowAdvisor(configurer);
	}

	public String getInitialWindowPerspectiveId() {
		return initialPerspective;
	}
}
