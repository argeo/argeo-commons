package org.argeo.cms.equinox.http.jetty;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.jetty.ee10.CmsJettyServer;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.rap.http.servlet.HttpServiceServlet;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;

/** A {@link CmsJettyServer} integrating with Equinox HTTP framework. */
public class EquinoxJettyServer extends CmsJettyServer {
	private final static CmsLog log = CmsLog.getLog(EquinoxJettyServer.class);
	private static final String INTERNAL_CONTEXT_CLASSLOADER = "org.eclipse.equinox.http.jetty.internal.ContextClassLoader";

	@Override
	protected void addServlets(ServletContextHandler servletContextHandler) {
		try {
			// servletContextHandler.setContextPath("/");
			// ServletHolder holder = new ServletHolder(new InternalHttpServiceServlet());
			ServletHolder holder = new ServletHolder(new HttpServiceServlet());
			holder.setInitOrder(0);
//			holder.setInitParameter(Constants.SERVICE_VENDOR, "Eclipse.org"); //$NON-NLS-1$
//			holder.setInitParameter(Constants.SERVICE_DESCRIPTION, "Equinox Jetty-based Http Service"); //$NON-NLS-1$

			// holder.setInitParameter("osgi.http.endpoint","/cms/user");
			servletContextHandler.addServlet(holder, "/*");

			// post-start
			SessionHandler sessionManager = servletContextHandler.getSessionHandler();
			sessionManager.addEventListener((HttpSessionIdListener) holder.getServlet());
		} catch (ServletException e) {
			throw new RuntimeException("Cannot add servlets", e);
		}
	}

	public static class InternalHttpServiceServlet implements HttpSessionListener, HttpSessionIdListener, Servlet {
		private final Servlet httpServiceServlet = new HttpServiceServlet();
		private ClassLoader contextLoader;
		private final Method sessionDestroyed;
		private final Method sessionIdChanged;

		public InternalHttpServiceServlet() {
			Class<?> clazz = httpServiceServlet.getClass();

			try {
				sessionDestroyed = clazz.getMethod("sessionDestroyed", new Class<?>[] { String.class }); //$NON-NLS-1$
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			try {
				sessionIdChanged = clazz.getMethod("sessionIdChanged", new Class<?>[] { String.class }); //$NON-NLS-1$
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public void init(ServletConfig config) throws ServletException {
			ServletContext context = config.getServletContext();
			contextLoader = (ClassLoader) context.getAttribute(INTERNAL_CONTEXT_CLASSLOADER);

			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(contextLoader);
			try {
				httpServiceServlet.init(config);
			} finally {
				thread.setContextClassLoader(current);
			}
		}

		@Override
		public void destroy() {
			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(contextLoader);
			try {
				httpServiceServlet.destroy();
			} finally {
				thread.setContextClassLoader(current);
			}
			contextLoader = null;
		}

		@Override
		public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(contextLoader);
			try {
				httpServiceServlet.service(req, res);
			} catch (IllegalStateException e) {
				// context is probably in shutdown
				if (log.isTraceEnabled())
					log.error("Cannot process request", e);
			} finally {
				thread.setContextClassLoader(current);
			}
		}

		@Override
		public ServletConfig getServletConfig() {
			return httpServiceServlet.getServletConfig();
		}

		@Override
		public String getServletInfo() {
			return httpServiceServlet.getServletInfo();
		}

		@Override
		public void sessionCreated(HttpSessionEvent event) {
			// Nothing to do.
		}

		@Override
		public void sessionDestroyed(HttpSessionEvent event) {
			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(contextLoader);
			try {
				sessionDestroyed.invoke(httpServiceServlet, event.getSession().getId());
			} catch (IllegalAccessException | IllegalArgumentException e) {
				// not likely
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e.getCause());
			} finally {
				thread.setContextClassLoader(current);
			}
		}

		@Override
		public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(contextLoader);
			try {
				sessionIdChanged.invoke(httpServiceServlet, oldSessionId);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				// not likely
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e.getCause());
			} finally {
				thread.setContextClassLoader(current);
			}
		}
	}

}
