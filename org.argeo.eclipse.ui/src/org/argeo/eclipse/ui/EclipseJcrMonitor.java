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
package org.argeo.eclipse.ui;

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
