/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
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

package org.argeo.jcr.mvc;

import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

public class OpenSessionInViewJcrInterceptor implements WebRequestInterceptor,
		JcrMvcConstants {
	private final static Log log = LogFactory
			.getLog(OpenSessionInViewJcrInterceptor.class);

	private Session session;

	public void preHandle(WebRequest request) throws Exception {
		if (log.isTraceEnabled())
			log.trace("preHandle: " + request);
		// Authentication auth = SecurityContextHolder.getContext()
		// .getAuthentication();
		// if (auth != null)
		// log.debug("auth=" + auth + ", authenticated="
		// + auth.isAuthenticated() + ", name=" + auth.getName());
		// else
		// log.debug("No auth");

		// FIXME: find a safer way to initialize
		// FIXME: not really needed to initialize here
		// session.getRepository();
		request.setAttribute(REQUEST_ATTR_SESSION, session,
				RequestAttributes.SCOPE_REQUEST);
	}

	public void postHandle(WebRequest request, ModelMap model) throws Exception {
		// if (log.isDebugEnabled())
		// log.debug("postHandle: " + request);
	}

	public void afterCompletion(WebRequest request, Exception ex)
			throws Exception {
		if (log.isTraceEnabled())
			log.trace("afterCompletion: " + request);
		// FIXME: only close session that were open
		session.logout();
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
