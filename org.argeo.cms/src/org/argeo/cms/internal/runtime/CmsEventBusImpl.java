package org.argeo.cms.internal.runtime;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;

import org.argeo.api.cms.CmsEventBus;
import org.argeo.api.cms.CmsEventSubscriber;
import org.argeo.api.cms.CmsLog;

/** {@link CmsEventBus} implementation based on {@link Flow}. */
public class CmsEventBusImpl implements CmsEventBus {
	private final static CmsLog log = CmsLog.getLog(CmsEventBus.class);

	private Map<String, SubmissionPublisher<Map<String, Object>>> topics = new TreeMap<>();

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

	/** A subscriber to a topic. */
	class CmsEventFlowSubscriber implements Flow.Subscriber<Map<String, Object>> {
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
			this.subscription.request(1);
		}

		@Override
		public void onNext(Map<String, Object> item) {
			eventSubscriber.onEvent(topic, item);
			this.subscription.request(1);
		}

		@Override
		public void onError(Throwable throwable) {
			if (throwable instanceof Error) {
				log.error("Unexpected error in event subscriber " + eventSubscriber + " for topic " + topic
						+ ", not trying to resubscribe.", throwable);
			} else {
				log.error("Unexpected exception in event subscriber " + eventSubscriber + " for topic " + topic
						+ ", resubscribing...", throwable);
				addEventSubscriber(topic, eventSubscriber);
			}
		}

		@Override
		public void onComplete() {
			if (log.isTraceEnabled())
				log.trace("Unexpected exception in event subscriber " + eventSubscriber + " for topic " + topic
						+ " is completed");
		}

		void unsubscribe() {
			if (subscription != null)
				subscription.cancel();
			else
				throw new IllegalStateException("No subscription to cancel");
		}

	}

}
