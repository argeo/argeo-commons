package org.argeo.eclipse.ui;

import org.argeo.ArgeoMonitor;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Wraps an Eclipse {@link IProgressMonitor} so that it can be passed to
 * framework agnostic Argeo routines.
 */
public class EclipseArgeoMonitor implements ArgeoMonitor {
	private final IProgressMonitor progressMonitor;

	public EclipseArgeoMonitor(IProgressMonitor progressMonitor) {
		this.progressMonitor = progressMonitor;
	}

	public void beginTask(String name, int totalWork) {
		progressMonitor.beginTask(name, totalWork);
	}

	public void done() {
		progressMonitor.done();
	}

	public boolean isCanceled() {
		return progressMonitor.isCanceled();
	}

	public void setCanceled(boolean value) {
		progressMonitor.setCanceled(value);
	}

	public void setTaskName(String name) {
		progressMonitor.setTaskName(name);
	}

	public void subTask(String name) {
		progressMonitor.subTask(name);
	}

	public void worked(int work) {
		progressMonitor.worked(work);
	}
}
