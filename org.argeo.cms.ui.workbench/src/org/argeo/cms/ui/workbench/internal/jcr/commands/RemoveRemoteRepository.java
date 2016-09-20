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
package org.argeo.cms.ui.workbench.internal.jcr.commands;

import org.argeo.cms.ui.workbench.internal.jcr.model.RemoteRepositoryElem;
import org.argeo.cms.ui.workbench.jcr.JcrBrowserView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/** Remove a registered remote repository */
public class RemoveRemoteRepository extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {

		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();

		JcrBrowserView view = (JcrBrowserView) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));

		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();

			if (obj instanceof RemoteRepositoryElem) {
				((RemoteRepositoryElem) obj).remove();
				view.refresh(null);
			}
		}
		return null;
	}

}
