package org.argeo.eclipse.ui.jcr;

import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

/** {@link EventListener} which simplifies running actions within the UI thread. */
public abstract class AsyncUiEventListener implements EventListener {
	private final Display display;
	
	public AsyncUiEventListener(Display display) {
		super();
		this.display = display;
	}

	/** Called asynchronously in the UI thread. */
	protected abstract void onEventInUiThread(EventIterator events);

	public void onEvent(final EventIterator events) {
		Job job = new Job("JCR Events") {
			protected IStatus run(IProgressMonitor monitor) {
				//Display display = Display.getCurrent();
				//Display display = PlatformUI.getWorkbench().getDisplay();

				display.asyncExec(new Runnable() {
					public void run() {
						onEventInUiThread(events);
					}
				});
				return Status.OK_STATUS;
			}
		};
		job.schedule();

		// PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
		// public void run() {
		// onEventInUiThread(events);
		// }
		// });
	}
}
