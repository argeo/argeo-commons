package org.argeo.eclipse.ui.jcr;

import org.argeo.jcr.JcrMonitor;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Wraps an Eclipse {@link IProgressMonitor} so that it can be passed to
 * framework agnostic Argeo routines.
 */
public class EclipseJcrMonitor implements JcrMonitor {
	private final IProgressMonitor progressMonitor;

	public EclipseJcrMonitor(IProgressMonitor progressMonitor) {
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
