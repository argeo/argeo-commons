package org.argeo.cms.internal.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.argeo.api.cms.CmsEventBus;
import org.argeo.api.cms.CmsEventSubscriber;
import org.argeo.api.cms.CmsLog;

/** A simple synchronous {@link CmsEventBus} implementation. */
public class CmsSynchronousEventBusImpl implements CmsEventBus {
	private final static CmsLog log = CmsLog.getLog(CmsSynchronousEventBusImpl.class);

	private final Map<String, List<CmsEventSubscriber>> subscribers;

	public CmsSynchronousEventBusImpl() {
		subscribers = Collections.synchronizedMap(new HashMap<>());
	}

	@Override
	public void sendEvent(String topic, Map<String, Object> event) {
		List<CmsEventSubscriber> subscribersOfTopic = subscribers.get(topic);
		if (subscribersOfTopic == null) // no one cares
			return;
		synchronized (subscribersOfTopic) {
			for (Iterator<CmsEventSubscriber> it = subscribersOfTopic.iterator(); it.hasNext();) {
				CmsEventSubscriber subscriber = it.next();
				try {
					subscriber.onEvent(topic, event);
				} catch (Throwable e) {
					log.error("Cannot process in topic " + topic + " the event " + event + " for subscriber "
							+ subscriber, e);
				}
			}
		}
		log.trace(() -> "Dispatched event in topic " + topic + ": " + event);
	}

	@Override
	public synchronized void addEventSubscriber(String topic, CmsEventSubscriber eventSubscriber) {
		if (!subscribers.containsKey(topic)) {
			subscribers.put(topic, new ArrayList<>());
		}
		subscribers.get(topic).add(eventSubscriber);
		log.debug(() -> "Added subscriber " + eventSubscriber + " to topic " + topic);
	}

	@Override
	public synchronized void removeEventSubscriber(String topic, CmsEventSubscriber eventSubscriber) {
		List<CmsEventSubscriber> subscribersOfTopic = subscribers.get(topic);
		assert subscribersOfTopic != null;
		if (subscribersOfTopic != null) {
			CmsEventSubscriber removedSubscriber = null;
			synchronized (subscribersOfTopic) {
				subscribersOfTopic: for (Iterator<CmsEventSubscriber> it = subscribersOfTopic.iterator(); it
						.hasNext();) {
					CmsEventSubscriber subscriber = it.next();
					if (subscriber == eventSubscriber) {
						it.remove();
						removedSubscriber = subscriber;
						log.debug(() -> "Removed subscriber " + eventSubscriber + " from topic " + topic);
						break subscribersOfTopic;
					}
				}
			}
			if (removedSubscriber == null)
				log.warn(() -> "Subscriber " + eventSubscriber + " not found (and therefore not removed) in topic "
						+ topic);
		}
	}

}
