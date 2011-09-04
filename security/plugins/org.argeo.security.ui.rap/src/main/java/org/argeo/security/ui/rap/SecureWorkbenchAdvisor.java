package org.argeo.security.ui.rap;

import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class SecureWorkbenchAdvisor extends WorkbenchAdvisor {
	//static final String DEFAULT_PERSPECTIVE_ID = "org.argeo.security.ui.adminSecurityPerspective"; //$NON-NLS-1$
	public final static String INITIAL_PERSPECTIVE_PROPERTY = "org.argeo.security.ui.initialPerspective";
	private String initialPerspective = System.getProperty(
			INITIAL_PERSPECTIVE_PROPERTY, null);

	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new SecureWorkbenchWindowAdvisor(configurer);
	}

	public String getInitialWindowPerspectiveId() {
		if (initialPerspective != null) {
			// check whether this user can see the declared perspective
			// (typically the perspective won't be listed if this user doesn't
			// have the right to see it)
			IPerspectiveDescriptor pd = getWorkbenchConfigurer().getWorkbench()
					.getPerspectiveRegistry()
					.findPerspectiveWithId(initialPerspective);
			if(pd==null)
				return null;
		}
		return initialPerspective;
	}
}
