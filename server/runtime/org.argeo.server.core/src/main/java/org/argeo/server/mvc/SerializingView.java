package org.argeo.server.mvc;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.ArgeoException;
import org.argeo.server.ServerAnswer;
import org.argeo.server.ServerSerializer;
import org.springframework.web.servlet.view.AbstractView;

public class SerializingView extends AbstractView implements MvcConstants {
	private final String viewName;
	private final Locale locale;

	private final ServerSerializer serializer;

	public SerializingView(String viewName, Locale locale,
			ServerSerializer serializer) {
		this.viewName = viewName;
		this.locale = locale;
		this.serializer = serializer;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void renderMergedOutputModel(Map model,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		Boolean serverAnswersAsHtml = false;
		final Object answer;
		if (model.size() == 1) {
			answer = model.values().iterator().next();
		} else if (model.containsKey(ANSWER_MODEL_KEY)) {
			answer = model.get(ANSWER_MODEL_KEY);
		} else if (model.containsKey(ANSWER_MODEL_KEY_AS_HTML)) {
			answer = model.get(ANSWER_MODEL_KEY_AS_HTML);
			serverAnswersAsHtml = true;
		} else if (model.containsKey(viewName)) {
			answer = model.get(viewName);
		} else {
			throw new ArgeoException(
					"Model has a size different from 1. Specify a modelKey.");
		}

		if ((answer instanceof ServerAnswer) && serverAnswersAsHtml) {
			response.setContentType("text/html");
			ServerAnswer serverAnswer = (ServerAnswer) answer;
			response.getWriter().append("<pre>");
			response.getWriter().append(serverAnswer.getMessage());
			response.getWriter().append("</pre>");
		} else {
			serializer.serialize(answer, request, response);
		}
	}

	public String getViewName() {
		return viewName;
	}

	public Locale getLocale() {
		return locale;
	}

}
