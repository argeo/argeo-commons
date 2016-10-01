package org.argeo.cms.internal.kernel;

import java.util.Map;

import javax.jcr.Repository;

import org.argeo.jcr.JcrRepositoryWrapper;
import org.argeo.node.DataModelNamespace;
import org.argeo.node.NodeConstants;
import org.osgi.framework.wiring.BundleCapability;

class LocalRepository extends JcrRepositoryWrapper {
	private final String cn;

	public LocalRepository(Repository repository, BundleCapability dataModelCapability) {
		super(repository);
		Map<String, Object> attrs = dataModelCapability.getAttributes();
		cn = (String) attrs.get(DataModelNamespace.CAPABILITY_NAME_ATTRIBUTE);
		putDescriptor(NodeConstants.CN, cn);
	}

	String getCn() {
		return cn;
	}

}
