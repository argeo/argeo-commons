package org.argeo.server.jackrabbit.webdav;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.springframework.web.servlet.DispatcherServlet;

public class ExtendedDispatcherServlet extends DispatcherServlet {
	private static final long serialVersionUID = 1L;

	private final static Log log = LogFactory
			.getLog(ExtendedDispatcherServlet.class);

	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException,
			java.io.IOException {
		// see http://forum.springsource.org/showthread.php?t=53472
		try {
			if (log.isTraceEnabled())
				log.trace("Received request " + request);
			doService(request, response);
		} catch (Exception e) {
			throw new ArgeoException("Cannot process request", e);
		}
	}

}
