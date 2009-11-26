package org.argeo.server.jackrabbit.webdav;

import java.io.IOException;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.argeo.ArgeoException;
import org.springframework.core.io.Resource;

public class WebDavServlet extends SimpleWebdavServlet {

	private static final long serialVersionUID = 1L;
	private final static Log log = LogFactory.getLog(WebDavServlet.class);

	private Repository repository;
	private Resource resourceConfiguration;

	public WebDavServlet() {

	}

	// private Session session;

	@Override
	public void init() throws ServletException {
		super.init();

		if (resourceConfiguration != null) {
			ResourceConfig resourceConfig = new ResourceConfig();
			try {
				resourceConfig.parse(resourceConfiguration.getURL());
			} catch (IOException e) {
				throw new ArgeoException("Cannot parse resource configuration "
						+ resourceConfiguration, e);
			}
			setResourceConfig(resourceConfig);
		}

		// try {
		// session().getWorkspace().getObservationManager().addEventListener(
		// this, Event.NODE_ADDED, "/", true, null, null, false);
		// if (log.isDebugEnabled())
		// log.debug("Registered listener");
		// } catch (Exception e) {
		// throw new ArgeoException("Cannot register event listener", e);
		// }
	}

	@Override
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		if (log.isDebugEnabled())
			log.debug("Received request with method '" + request.getMethod()
					+ "'");
		super.service(request, response);

		if (log.isDebugEnabled()) {
			log.debug("Webdav response: " + response);
			// response.
		}
	}

	// public void onEvent(EventIterator events) {
	// while (events.hasNext()) {
	// Event event = events.nextEvent();
	// log.debug(event);
	// }
	//
	// }

	// protected Session session() {
	// if (session == null)
	// try {
	// session = getRepository().login(
	// new SimpleCredentials("demo", "demo".toCharArray()));
	// } catch (Exception e) {
	// throw new ArgeoException("Cannot open session", e);
	// }
	// return session;
	// }

	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setResourceConfiguration(Resource resourceConfig) {
		this.resourceConfiguration = resourceConfig;
	}

}
