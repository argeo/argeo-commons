package org.argeo.cms.internal.kernel;

import javax.jcr.Repository;

import org.argeo.jcr.JcrRepositoryWrapper;
import org.argeo.node.NodeConstants;

class LocalRepository extends JcrRepositoryWrapper {
	private final String cn;

	public LocalRepository(Repository repository, String cn) {
		super(repository);
		this.cn = cn;
		// Map<String, Object> attrs = dataModelCapability.getAttributes();
		// cn = (String) attrs.get(DataModelNamespace.NAME);
		putDescriptor(NodeConstants.CN, cn);
	}

	String getCn() {
		return cn;
	}

}
