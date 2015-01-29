package org.argeo.security.ui;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Propagate authentication to an eclipse job. Typically to execute a privileged
 * action outside the UI thread
 */
public abstract class PrivilegedJob extends Job {

	private final Authentication authentication;
	private Subject subject;

	public PrivilegedJob(String jobName) {
		super(jobName);
		authentication = SecurityContextHolder.getContext().getAuthentication();
		subject = Subject.getSubject(AccessController.getContext());

		// Must be called *before* the job is scheduled,
		// it is required for the progress window to appear
		setUser(true);
	}

	@Override
	protected IStatus run(final IProgressMonitor progressMonitor) {
		PrivilegedAction<IStatus> privilegedAction = new PrivilegedAction<IStatus>() {
			public IStatus run() {
				SecurityContextHolder.getContext().setAuthentication(
						authentication);
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