package org.argeo.api.cms;

import java.util.Dictionary;

/** A configured node deployment. */
public interface CmsDeployment {

	void addFactoryDeployConfig(String factoryPid, Dictionary<String, Object> props);

	Dictionary<String, Object> getProps(String factoryPid, String cn);
}
