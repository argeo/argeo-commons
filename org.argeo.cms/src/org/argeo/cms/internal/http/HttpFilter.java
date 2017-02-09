package org.argeo.cms.internal.http;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Abstract base class for http filters. */
abstract class HttpFilter implements Filter {
	// private final static Log log = LogFactory.getLog(HttpFilter.class);

	protected abstract void doFilter(HttpSession httpSession,
			HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws IOException, ServletException;

	@Override
	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
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
