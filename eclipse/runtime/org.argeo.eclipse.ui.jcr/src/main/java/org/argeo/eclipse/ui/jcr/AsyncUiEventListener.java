package org.argeo.eclipse.ui.jcr;

import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.eclipse.ui.PlatformUI;

/** {@link EventListener} which simplifies running actions within the UI thread. */
public abstract class AsyncUiEventListener implements EventListener {
	/** Called asynchronously in the UI thread. */
	protected abstract void onEventInUiThread(EventIterator events);

	public void onEvent(final EventIterator events) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				onEventInUiThread(events);
			}
		});
	}
}
