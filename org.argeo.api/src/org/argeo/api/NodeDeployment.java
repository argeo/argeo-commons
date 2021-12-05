package org.argeo.api;

import java.util.Dictionary;

/** A configured node deployment. */
public interface NodeDeployment {
	Long getAvailableSince();
	
	void addFactoryDeployConfig(String factoryPid, Dictionary<String, Object> props);
	Dictionary<String, Object> getProps(String factoryPid, String cn);

}
