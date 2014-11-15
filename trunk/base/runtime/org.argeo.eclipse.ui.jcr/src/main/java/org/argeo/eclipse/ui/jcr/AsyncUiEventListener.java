/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.eclipse.ui.jcr;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.eclipse.swt.widgets.Display;

/** {@link EventListener} which simplifies running actions within the UI thread. */
public abstract class AsyncUiEventListener implements EventListener {
//	private final static Log logSuper = LogFactory
//			.getLog(AsyncUiEventListener.class);
	private final Log logThis = LogFactory.getLog(getClass());

	private final Display display;

	public AsyncUiEventListener(Display display) {
		super();
		this.display = display;
	}

	/** Called asynchronously in the UI thread. */
	protected abstract void onEventInUiThread(List<Event> events)
			throws RepositoryException;

	/**
	 * Whether these events should be processed in the UI or skipped with no UI
	 * job created.
	 */
	protected Boolean willProcessInUiThread(List<Event> events)
			throws RepositoryException {
		return true;
	}

	protected Log getLog() {
		return logThis;
	}

	public final void onEvent(final EventIterator eventIterator) {
		final List<Event> events = new ArrayList<Event>();
		while (eventIterator.hasNext())
			events.add(eventIterator.nextEvent());

		if (logThis.isTraceEnabled())
			logThis.trace("Received " + events.size() + " events");

		try {
			if (!willProcessInUiThread(events))
				return;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot test skip events " + events, e);
		}

//		Job job = new Job("JCR Events") {
//			protected IStatus run(IProgressMonitor monitor) {
//				if (display.isDisposed()) {
//					logSuper.warn("Display is disposed cannot update UI");
//					return Status.CANCEL_STATUS;
//				}

				display.asyncExec(new Runnable() {
					public void run() {
						try {
							onEventInUiThread(events);
						} catch (RepositoryException e) {
							throw new ArgeoException("Cannot process events "
									+ events, e);
						}
					}
				});

//				return Status.OK_STATUS;
//			}
//		};
//		job.schedule();
	}
}
