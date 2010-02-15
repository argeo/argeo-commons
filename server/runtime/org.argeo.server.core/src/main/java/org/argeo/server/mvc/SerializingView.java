package org.argeo.server.mvc;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.ArgeoException;
import org.argeo.server.ServerAnswer;
import org.argeo.server.ServerSerializer;
import org.springframework.validation.BindingResult;
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
		Boolean serverAnswersAsHtml = model
				.containsKey(ANSWER_MODEL_KEY_AS_HTML);

		final Object answer = findAnswerInModel(model);

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

	@SuppressWarnings("unchecked")
	protected Object findAnswerInModel(Map model) {
		if (model.size() == 1) {
			return model.values().iterator().next();
		} else if (model.size() == 2) {
			boolean otherIsBindingResult = false;
			Object answerValue = null;
			for (Object value : model.values()) {
				if (value instanceof BindingResult)
					otherIsBindingResult = true;
				else
					answerValue = value;
			}

			if (otherIsBindingResult)
				return answerValue;
		}

		if (model.containsKey(ANSWER_MODEL_KEY)) {
			return model.get(ANSWER_MODEL_KEY);
		} else if (model.containsKey(ANSWER_MODEL_KEY_AS_HTML)) {
			return model.get(ANSWER_MODEL_KEY_AS_HTML);
		} else if (model.containsKey(viewName)) {
			return model.get(viewName);
		} else {
			if (model.size() == 0)
				throw new ArgeoException("Model is empty.");
			else
				throw new ArgeoException(
						"Model has a size different from 1. Specify a modelKey.");
		}
	}

	public String getViewName() {
		return viewName;
	}

	public Locale getLocale() {
		return locale;
	}

}
