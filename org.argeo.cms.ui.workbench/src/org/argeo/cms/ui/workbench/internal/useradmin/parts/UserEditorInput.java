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
package org.argeo.cms.ui.workbench.internal.useradmin.parts;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * Editor input for an user defined by unique name (usually a distinguished
 * name).
 */
public class UserEditorInput implements IEditorInput {
	private final String username;

	public UserEditorInput(String username) {
		this.username = username;
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	public boolean exists() {
		return username != null;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return username != null ? username : "<new user>";
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return username != null ? username : "<new user>";
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof UserEditorInput))
			return false;
		if (((UserEditorInput) obj).getUsername() == null)
			return false;
		return ((UserEditorInput) obj).getUsername().equals(username);
	}

	public String getUsername() {
		return username;
	}
}