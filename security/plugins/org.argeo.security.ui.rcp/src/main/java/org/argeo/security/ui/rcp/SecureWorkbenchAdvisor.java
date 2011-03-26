package org.argeo.security.ui.rcp;

import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class SecureWorkbenchAdvisor extends WorkbenchAdvisor {
	static final String DEFAULT_PERSPECTIVE_ID = "org.argeo.security.ui.adminSecurityPerspective"; //$NON-NLS-1$
	public final static String INITIAL_PERSPECTIVE_PROPERTY = "org.argeo.security.ui.initialPerspective";

	private final String username;
	private String initialPerspective = System.getProperty(
			INITIAL_PERSPECTIVE_PROPERTY, DEFAULT_PERSPECTIVE_ID);

	public SecureWorkbenchAdvisor(String username) {
		super();
		this.username = username;
	}

	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new SecureWorkbenchWindowAdvisor(configurer, username);
	}

	public String getInitialWindowPerspectiveId() {
		return initialPerspective;
	}
}
