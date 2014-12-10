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
package org.argeo.eclipse.ui.workbench.jcr.internal.parts;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.eclipse.jface.wizard.Wizard;

/**
 * Small wizard to manage authorizations on the root node of the current
 * workspace
 */
public class ChangeRightsWizard extends Wizard {

	private Session currentSession;
	private String path;

	// This page widget
	private ChooseRightsPage page;

	public ChangeRightsWizard(Session currentSession, String path) {
		super();
		this.currentSession = currentSession;
		this.path = path;
	}

	@Override
	public void addPages() {
		try {
			page = new ChooseRightsPage(path);
			addPage(page);
		} catch (Exception e) {
			throw new ArgeoException("Cannot add page to wizard ", e);
		}
	}

	@Override
	public boolean performFinish() {
		if (!canFinish())
			return false;
		try {
			JcrUtils.addPrivilege(currentSession, path, page.getGroupName(),
					page.getAuthTypeStr());
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while setting privileges", re);
		}
		return true;
	}
}
