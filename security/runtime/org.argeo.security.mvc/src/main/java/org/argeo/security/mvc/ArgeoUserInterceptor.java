package org.argeo.security.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.security.ArgeoSecurityService;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/** Add the current argeo user as an attribute to the request. */
public class ArgeoUserInterceptor extends HandlerInterceptorAdapter {
	private ArgeoSecurityService securityService;

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		request.setAttribute("argeoUser", securityService.getCurrentUser());
		return super.preHandle(request, response, handler);
	}

	public void setSecurityService(ArgeoSecurityService securityService) {
		this.securityService = securityService;
	}

}
