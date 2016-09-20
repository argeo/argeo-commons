package org.argeo.cms.ui.workbench.util;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Propagate authentication to an eclipse job. Typically to execute a privileged
 * action outside the UI thread
 */
public abstract class PrivilegedJob extends Job {
	private final Subject subject;

	public PrivilegedJob(String jobName) {
		this(jobName, AccessController.getContext());
	}

	public PrivilegedJob(String jobName,
			AccessControlContext accessControlContext) {
		super(jobName);
		subject = Subject.getSubject(accessControlContext);

		// Must be called *before* the job is scheduled,
		// it is required for the progress window to appear
		setUser(true);
	}

	@Override
	protected IStatus run(final IProgressMonitor progressMonitor) {
		PrivilegedAction<IStatus> privilegedAction = new PrivilegedAction<IStatus>() {
			public IStatus run() {
				return doRun(progressMonitor);
			}
		};
		return Subject.doAs(subject, privilegedAction);
	}

	/**
	 * Implement here what should be executed with default context
	 * authentication
	 */
	protected abstract IStatus doRun(IProgressMonitor progressMonitor);
}