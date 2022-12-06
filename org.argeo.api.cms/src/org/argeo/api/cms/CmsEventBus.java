package org.argeo.api.cms;

import java.util.Map;

public interface CmsEventBus {
	void sendEvent(String topic, Map<String, Object> event);

	void addEventSubscriber(String topic, CmsEventSubscriber eventSubscriber);

	void removeEventSubscriber(String topic, CmsEventSubscriber eventSubscriber);

}
