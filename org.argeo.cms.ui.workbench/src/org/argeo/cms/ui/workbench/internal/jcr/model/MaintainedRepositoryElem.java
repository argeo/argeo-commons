package org.argeo.cms.ui.workbench.internal.jcr.model;

import javax.jcr.Repository;

import org.argeo.eclipse.ui.TreeParent;

/** Wrap a MaintainedRepository */
public class MaintainedRepositoryElem extends RepositoryElem {

	public MaintainedRepositoryElem(String alias, Repository repository, TreeParent parent) {
		super(alias, repository, parent);
		// if (!(repository instanceof MaintainedRepository)) {
		// throw new ArgeoException("Repository " + alias
		// + " is not amiantained repository");
		// }
	}

	// protected MaintainedRepository getMaintainedRepository() {
	// return (MaintainedRepository) getRepository();
	// }
}
