/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.ArgeoException;
import org.argeo.server.ServerAnswer;
import org.argeo.server.ServerSerializer;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

/**
 * Can be used as a standalone {@link View} or using
 * {@link SerializingViewResolver}
 */
public class SerializingView extends AbstractView implements MvcConstants {
	private final String viewName;
	private final Locale locale;

	private ServerSerializer serializer;

	public SerializingView() {
		this.viewName = null;
		this.locale = Locale.getDefault();
	}

	public SerializingView(String viewName, Locale locale,
			ServerSerializer serializer) {
		this.viewName = viewName;
		this.locale = locale;
		this.serializer = serializer;
	}

	@SuppressWarnings( { "restriction", "rawtypes" })
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

	@SuppressWarnings("rawtypes")
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
		} else if (viewName != null && model.containsKey(viewName)) {
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

	public void setSerializer(ServerSerializer serializer) {
		this.serializer = serializer;
	}

}
