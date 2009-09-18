package org.argeo.server.json;

import java.io.BufferedReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.server.ArgeoServerException;
import org.argeo.server.ServerDeserializer;
import org.argeo.server.ServerSerializer;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;

public class JsonServerMapper implements ServerSerializer, ServerDeserializer {
	private JsonFactory jsonFactory = new JsonFactory();
	private ObjectMapper objectMapper = new ObjectMapper();

	public void serialize(Object obj, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			response.setContentType("application/json");

			JsonGenerator jsonGenerator = jsonFactory
					.createJsonGenerator(response.getWriter());
			jsonGenerator.useDefaultPrettyPrinter();

			objectMapper.writeValue(jsonGenerator, obj);
		} catch (Exception e) {
			throw new ArgeoServerException("Cannot serialize " + obj, e);
		}
	}

	public Object deserialize(Object obj, HttpServletRequest request) {
		try {
			String body = request.getParameter("body");
			if (body == null) {
				// lets read the message body instead
				BufferedReader reader = request.getReader();
				StringBuffer buffer = new StringBuffer();
				String line = null;
				while (((line = reader.readLine()) != null)) {
					buffer.append(line);
				}
				body = buffer.toString();
			}
			return objectMapper.readValue(body, Object.class);
		} catch (Exception e) {
			throw new ArgeoServerException("Cannot deserialize " + request, e);
		}
	}

}
