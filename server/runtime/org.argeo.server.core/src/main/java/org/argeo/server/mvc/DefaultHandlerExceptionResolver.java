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

package org.argeo.server.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.ServerAnswer;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

public class DefaultHandlerExceptionResolver implements
		HandlerExceptionResolver {
	private final static Log log = LogFactory
			.getLog(DefaultHandlerExceptionResolver.class);

	public ModelAndView resolveException(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex) {
		ModelAndView mv = new ModelAndView();
		ServerAnswer serverAnswer = ServerAnswer.error(ex);
		mv.addObject(serverAnswer);

		if (log.isDebugEnabled())
			log.error(serverAnswer);

		mv.setViewName("500");
		// response.setStatus(500);
		return mv;
	}

}
