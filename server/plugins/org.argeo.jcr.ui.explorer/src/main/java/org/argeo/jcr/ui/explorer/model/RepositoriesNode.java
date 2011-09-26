package org.argeo.jcr.ui.explorer.model;

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;

import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.RepositoryRegister;

/**
 * UI Tree component. Implements the Argeo abstraction of a
 * {@link RepositoryFactory} that enable a user to "mount" various repositories
 * in a single Tree like View. It is usually meant to be at the root of the UI
 * Tree and thus {@link getParent()} method will return null.
 * 
 * The {@link RepositoryFactory} is injected at instantiation time and must be
 * use get or register new {@link Repository} objects upon which a reference is
 * kept here.
 */

public class RepositoriesNode extends TreeParent {
	private final RepositoryRegister repositoryRegister;

	public RepositoriesNode(String name, RepositoryRegister repositoryRegister,
			TreeParent parent) {
		super(name);
		this.repositoryRegister = repositoryRegister;
	}

	/**
	 * Override normal behaviour to initialize the various repositories only at
	 * request time
	 */
	@Override
	public synchronized Object[] getChildren() {
		if (isLoaded()) {
			return super.getChildren();
		} else {
			// initialize current object
			Map<String, Repository> refRepos = repositoryRegister
					.getRepositories();
			for (String name : refRepos.keySet()) {
				super.addChild(new RepositoryNode(name, refRepos.get(name),
						this));
			}
			return super.getChildren();
		}
	}

	public void registerNewRepository(String alias, Repository repository) {
		// TODO: implement this
		// Create a new RepositoryNode Object
		// add it
		// super.addChild(new RepositoriesNode(...));
	}

	/** Returns the {@link RepositoryRegister} wrapped by thgis object. */
	public RepositoryRegister getRepositoryRegister() {
		return repositoryRegister;
	}
}
