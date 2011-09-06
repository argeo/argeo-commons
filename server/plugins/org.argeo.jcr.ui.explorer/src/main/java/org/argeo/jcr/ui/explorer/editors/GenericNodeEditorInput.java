package org.argeo.jcr.ui.explorer.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * An editor input based on a path to a node plus workspace name and repository
 * alias. In a multirepository environment, path can be enriched with Repository
 * Alias and workspace
 */

public class GenericNodeEditorInput implements IEditorInput {
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
	public GenericNodeEditorInput(String repositoryAlias, String workspaceName,
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

		GenericNodeEditorInput other = (GenericNodeEditorInput) obj;

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
