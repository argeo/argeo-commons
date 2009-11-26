package org.argeo.server.jcr;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
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
	private Repository repository;
	private String path = "/";
	private Boolean deep = true;
	private String username = "demo";
	private String password = "demo";

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

	protected Session session() {
		if (session == null)
			try {
				session = repository.login(new SimpleCredentials(username,
						password.toCharArray()));
			} catch (Exception e) {
				throw new ArgeoException("Cannot open session", e);
			}
		return session;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
