package org.argeo.server.json;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.ArgeoServerException;
import org.argeo.server.ServerSerializer;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;

public class JsonServerSerializer implements ServerSerializer {
	private final static Log log = LogFactory
			.getLog(JsonServerSerializer.class);

	private JsonFactory jsonFactory = new JsonFactory();
	private ObjectMapper objectMapper = new ObjectMapper();

	private Boolean prettyPrint = true;

	public void serialize(Object obj, HttpServletRequest request,
			HttpServletResponse response) {
		JsonGenerator jsonGenerator = null;
		try {
			response.setContentType("application/json");

			StringWriter stringWriter = null;
			if (log.isTraceEnabled()) {
				stringWriter = new StringWriter();
				jsonGenerator = jsonFactory.createJsonGenerator(stringWriter);
			} else {
				jsonGenerator = jsonFactory.createJsonGenerator(response
						.getWriter());
			}

			if (prettyPrint)
				jsonGenerator.useDefaultPrettyPrinter();

			objectMapper.writeValue(jsonGenerator, obj);

			jsonGenerator.close();

			if (stringWriter != null) {
				if (log.isTraceEnabled())
					log.debug(stringWriter.toString());
				response.getWriter().append(stringWriter.toString());
			}

		} catch (Exception e) {
			throw new ArgeoServerException("Cannot serialize " + obj, e);
		} finally {
			if (jsonGenerator != null)
				try {
					jsonGenerator.close();
				} catch (IOException e) {
					if (log.isTraceEnabled())
						log.error("Cannot close JSON generator", e);
				}
		}
	}

	public void setPrettyPrint(Boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}

	protected ObjectMapper getObjectMapper() {
		return objectMapper;
	}
}
