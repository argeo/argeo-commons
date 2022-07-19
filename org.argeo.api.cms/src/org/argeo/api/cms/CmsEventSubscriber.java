package org.argeo.api.cms;

import java.util.Map;

public interface CmsEventSubscriber {

	void onEvent(String topic, Map<String, Object> properties);
}
