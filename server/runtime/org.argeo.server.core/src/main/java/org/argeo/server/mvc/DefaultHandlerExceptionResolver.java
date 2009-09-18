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
