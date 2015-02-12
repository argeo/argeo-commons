package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Abstract base class for http filters. */
abstract class HttpFilter implements Filter {
	private final static Log log = LogFactory.getLog(HttpFilter.class);

	protected abstract void doFilter(HttpSession httpSession,
			HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws IOException, ServletException;

	@Override
	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		if (log.isDebugEnabled()) {
			log.debug(request.getContextPath());
			log.debug(request.getServletPath());
			log.debug(request.getRequestURI());
			log.debug(request.getQueryString());
			StringBuilder buf = new StringBuilder();
			Enumeration<String> en = request.getHeaderNames();
			while (en.hasMoreElements()) {
				String header = en.nextElement();
				Enumeration<String> values = request.getHeaders(header);
				while (values.hasMoreElements())
					buf.append("  " + header + ": " + values.nextElement());
				buf.append('\n');
			}
			log.debug(buf);
		}

		doFilter(request.getSession(), request,
				(HttpServletResponse) servletResponse, filterChain);
	}

	@Override
	public void destroy() {
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

}
