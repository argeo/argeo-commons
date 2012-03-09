/*
 * Copyright (C) 2007-2012 Mathieu Baudier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;

/** To be overridden */
public class DefaultJcrListener implements EventListener {
	private final static Log log = LogFactory.getLog(DefaultJcrListener.class);
	private Session session;
	private String path = "/";
	private Boolean deep = true;

	public void start() {
		try {
			addEventListener(session().getWorkspace().getObservationManager());
			if (log.isDebugEnabled())
				log.debug("Registered JCR event listener on " + path);
		} catch (Exception e) {
			throw new ArgeoException("Cannot register event listener", e);
		}
	}

	public void stop() {
		try {
			session().getWorkspace().getObservationManager()
					.removeEventListener(this);
			if (log.isDebugEnabled())
				log.debug("Unregistered JCR event listener on " + path);
		} catch (Exception e) {
			throw new ArgeoException("Cannot unregister event listener", e);
		}
	}

	/** Default is listen to all events */
	protected Integer getEvents() {
		return Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED
				| Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
	}

	/** To be overidden */
	public void onEvent(EventIterator events) {
		while (events.hasNext()) {
			Event event = events.nextEvent();
			log.debug(event);
		}
	}

	/** To be overidden */
	protected void addEventListener(ObservationManager observationManager)
			throws RepositoryException {
		observationManager.addEventListener(this, getEvents(), path, deep,
				null, null, false);
	}

	private Session session() {
		return session;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setDeep(Boolean deep) {
		this.deep = deep;
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
