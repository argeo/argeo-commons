package org.argeo.cms.internal.runtime;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;

import org.argeo.api.cms.CmsEventBus;
import org.argeo.api.cms.CmsEventSubscriber;
import org.argeo.api.cms.CmsLog;

public class CmsEventBusImpl implements CmsEventBus {
	private final CmsLog log = CmsLog.getLog(CmsEventBus.class);

	// CMS events
	private Map<String, SubmissionPublisher<Map<String, Object>>> topics = new TreeMap<>();
//	private IdentityHashMap<CmsEventSubscriber, List<CmsEventFlowSubscriber>> subscriptions = new IdentityHashMap<>();

	/*
	 * CMS Events
	 */
	@Override
	public void sendEvent(String topic, Map<String, Object> event) {
		SubmissionPublisher<Map<String, Object>> publisher = topics.get(topic);
		if (publisher == null)
			return; // no one is interested
		publisher.submit(event);
	}

	@Override
	public void addEventSubscriber(String topic, CmsEventSubscriber subscriber) {
		synchronized (topics) {
			if (!topics.containsKey(topic))
				topics.put(topic, new SubmissionPublisher<>());
		}
		SubmissionPublisher<Map<String, Object>> publisher = topics.get(topic);
		CmsEventFlowSubscriber flowSubscriber = new CmsEventFlowSubscriber(topic, subscriber);
		publisher.subscribe(flowSubscriber);
	}

	@Override
	public void removeEventSubscriber(String topic, CmsEventSubscriber subscriber) {
		SubmissionPublisher<Map<String, Object>> publisher = topics.get(topic);
		if (publisher == null) {
			log.error("There should be an event topic " + topic);
			return;
		}
		for (Flow.Subscriber<? super Map<String, Object>> flowSubscriber : publisher.getSubscribers()) {
			if (flowSubscriber instanceof CmsEventFlowSubscriber)
				((CmsEventFlowSubscriber) flowSubscriber).unsubscribe();
		}
		synchronized (topics) {
			if (!publisher.hasSubscribers()) {
				publisher.close();
				topics.remove(topic);
			}
		}
	}

	static class CmsEventFlowSubscriber implements Flow.Subscriber<Map<String, Object>> {
		private String topic;
		private CmsEventSubscriber eventSubscriber;

		private Subscription subscription;

		public CmsEventFlowSubscriber(String topic, CmsEventSubscriber eventSubscriber) {
			this.topic = topic;
			this.eventSubscriber = eventSubscriber;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
			this.subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Map<String, Object> item) {
			eventSubscriber.onEvent(topic, item);
		}

		@Override
		public void onError(Throwable throwable) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onComplete() {
			// TODO Auto-generated method stub

		}

		void unsubscribe() {
			if (subscription != null)
				subscription.cancel();
			else
				throw new IllegalStateException("No subscription to cancel");
		}

	}

}