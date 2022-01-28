package org.argeo.cms.jcr.internal;

import java.util.Dictionary;

import javax.jcr.Repository;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

class NodeKeyRing extends JcrKeyring implements ManagedService{
	
	public NodeKeyRing(Repository repository) {
		super(repository);
	}

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
	}
}
