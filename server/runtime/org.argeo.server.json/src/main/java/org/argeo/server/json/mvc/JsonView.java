package org.argeo.server.json.mvc;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.server.ArgeoServerException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.web.servlet.view.AbstractView;

/** Marshal one of the object of the map to the output. */
public class JsonView extends AbstractView {
	private JsonFactory jsonFactory = new JsonFactory();
	private ObjectMapper objectMapper = new ObjectMapper();

	private String modelKey = null;

	public JsonView(String modelKey) {
		this.modelKey = modelKey;
	}

	@Override
	@SuppressWarnings(value = { "unchecked" })
	protected void renderMergedOutputModel(Map model,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		final Object answer;
		if (model.size() == 1)
			answer = model.values().iterator().next();
		else if (modelKey != null) {
			if (!model.containsKey(modelKey))
				throw new ArgeoServerException("Key " + modelKey
						+ " not found in model.");
			answer = model.get(modelKey);
		} else {// models.size!=1 and no modelKey
			throw new ArgeoServerException(
					"Model has a size different from 1. Specify a modelKey.");
		}

		response.setContentType("application/json");

		JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(response
				.getWriter());
		jsonGenerator.useDefaultPrettyPrinter();

		objectMapper.writeValue(jsonGenerator, answer);

	}

	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
	}

}
