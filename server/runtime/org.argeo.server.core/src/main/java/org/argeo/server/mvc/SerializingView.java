package org.argeo.server.mvc;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.server.ArgeoServerException;
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
		final Object answer;
		if (model.size() == 1) {
			answer = model.values().iterator().next();
		} else if (model.containsKey(ANSWER_MODEL_KEY)) {
			answer = model.get(ANSWER_MODEL_KEY);
		} else if (model.containsKey(viewName)) {
			answer = model.get(viewName);
		} else {
			throw new ArgeoServerException(
					"Model has a size different from 1. Specify a modelKey.");
		}

		serializer.serialize(answer, request, response);
	}

	public String getViewName() {
		return viewName;
	}

	public Locale getLocale() {
		return locale;
	}

}
