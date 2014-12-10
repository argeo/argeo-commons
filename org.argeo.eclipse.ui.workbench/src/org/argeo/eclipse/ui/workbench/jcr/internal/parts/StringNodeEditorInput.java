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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * An editor input based on three strings define a node :
 * <ul>
 * <li>complete path to the node</li>
 * <li>the workspace name</li>
 * <li>the repository alias</li>
 * </ul>
 * In a single workspace and/or repository environment, name and alias can be
 * null.
 * 
 * Note : unused for the time being.
 */

public class StringNodeEditorInput implements IEditorInput {
	private final String path;
	private final String repositoryAlias;
	private final String workspaceName;

	/**
	 * In order to implement a generic explorer that supports remote and multi
	 * workspaces repositories, node path can be detailed by these strings.
	 * 
	 * @param repositoryAlias
	 *            : can be null
	 * @param workspaceName
	 *            : can be null
	 * @param path
	 */
	public StringNodeEditorInput(String repositoryAlias, String workspaceName,
			String path) {
		this.path = path;
		this.repositoryAlias = repositoryAlias;
		this.workspaceName = workspaceName;
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	public boolean exists() {
		return true;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return path;
	}

	public String getRepositoryAlias() {
		return repositoryAlias;
	}

	public String getWorkspaceName() {
		return workspaceName;
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return path;
	}

	public String getPath() {
		return path;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		StringNodeEditorInput other = (StringNodeEditorInput) obj;

		if (!path.equals(other.getPath()))
			return false;

		String own = other.getWorkspaceName();
		if ((workspaceName == null && own != null)
				|| (workspaceName != null && (own == null || !workspaceName
						.equals(own))))
			return false;

		String ora = other.getRepositoryAlias();
		if ((repositoryAlias == null && ora != null)
				|| (repositoryAlias != null && (ora == null || !repositoryAlias
						.equals(ora))))
			return false;

		return true;
	}
}
