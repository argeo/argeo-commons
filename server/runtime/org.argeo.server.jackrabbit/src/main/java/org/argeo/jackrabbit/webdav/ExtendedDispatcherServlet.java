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

package org.argeo.jackrabbit.webdav;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.springframework.web.servlet.DispatcherServlet;

public class ExtendedDispatcherServlet extends DispatcherServlet {
	private static final long serialVersionUID = -5584673209855752009L;

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
