package org.argeo.jcr.ui.explorer.model;

import javax.jcr.Repository;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.MaintainedRepository;

/** Wraps a {@link MaintainedRepository} */
public class MaintainedRepositoryElem extends RepositoryElem {

	public MaintainedRepositoryElem(String alias, Repository repository,
			TreeParent parent) {
		super(alias, repository, parent);
		if (!(repository instanceof MaintainedRepository)) {
			throw new ArgeoException("Repository " + alias
					+ " is not amiantained repository");
		}
	}

	protected MaintainedRepository getMaintainedRepository() {
		return (MaintainedRepository) getRepository();
	}
}
