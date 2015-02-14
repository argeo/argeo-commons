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
package org.argeo.eclipse.ui.workbench;

import org.eclipse.swt.widgets.Shell;

/** @deprecated Use {@link org.argeo.eclipse.ui.dialogs.ErrorFeedback} instead. */
@Deprecated
public class ErrorFeedback extends org.argeo.eclipse.ui.dialogs.ErrorFeedback {
	private static final long serialVersionUID = 5346084648745909554L;

	public ErrorFeedback(Shell parentShell, String message, Throwable e) {
		super(parentShell, message, e);
	}

}