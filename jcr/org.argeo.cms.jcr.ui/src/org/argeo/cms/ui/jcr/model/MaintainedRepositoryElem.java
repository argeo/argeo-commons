package org.argeo.cms.ui.jcr.model;

import javax.jcr.Repository;

import org.argeo.cms.ux.widgets.TreeParent;

/** Wrap a MaintainedRepository */
public class MaintainedRepositoryElem extends RepositoryElem {

	public MaintainedRepositoryElem(String alias, Repository repository, TreeParent parent) {
		super(alias, repository, parent);
		// if (!(repository instanceof MaintainedRepository)) {
		// throw new ArgeoException("Repository " + alias
		// + " is not a maintained repository");
		// }
	}

	// protected MaintainedRepository getMaintainedRepository() {
	// return (MaintainedRepository) getRepository();
	// }
}
