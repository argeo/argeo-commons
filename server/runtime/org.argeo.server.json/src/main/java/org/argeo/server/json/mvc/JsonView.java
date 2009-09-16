package org.argeo.slc.web.mvc;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.view.AbstractView;

/** Marshal one of the object of the map to the output. */
public class JsonView extends AbstractView {
	private String modelKey = null;

	public JsonView() {
	}

	@Override
	@SuppressWarnings(value = { "unchecked" })
	protected void renderMergedOutputModel(Map model,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		final Object answer;
		if (modelKey != null) {
			if (!model.containsKey(modelKey))
				throw new SlcException("Key " + modelKey
						+ " not found in model.");
			answer = model.get(modelKey);
		} else {
			if (model.size() != 1)
				throw new SlcException(
						"Model has a size different from 1. Specify a modelKey.");
			answer = model.values().iterator().next();
		}

		if (answer instanceof JSONObject) {
			((JSONObject) answer).write(response.getWriter());
		} else {
			JSONObject jsonObject = new JSONObject(answer);
			jsonObject.write(response.getWriter());
		}
	}

	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
	}

}
