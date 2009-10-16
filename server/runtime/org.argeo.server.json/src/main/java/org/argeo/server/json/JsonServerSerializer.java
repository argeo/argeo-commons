package org.argeo.server.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.ArgeoServerException;
import org.argeo.server.Serializer;
import org.argeo.server.ServerSerializer;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;

public class JsonServerSerializer implements ServerSerializer, Serializer {
	private final static Log log = LogFactory
			.getLog(JsonServerSerializer.class);

	private JsonFactory jsonFactory = new JsonFactory();
	private ObjectMapper objectMapper = new ObjectMapper();

	private Boolean prettyPrint = false;

	// private String encoding = "UTF8";

	public void serialize(Object obj, HttpServletRequest request,
			HttpServletResponse response) {
		response.setContentType("application/json");
		try {
			serialize(response.getWriter(), obj);
		} catch (IOException e) {
			throw new ArgeoServerException("Cannot open response stream.", e);
		}
	}

	public void serialize(Writer writer, Object obj) {
		serializeAndLog(obj);

		JsonGenerator jsonGenerator = null;
		try {

			// jsonGenerator = jsonFactory.createJsonGenerator(response
			// .getOutputStream(), JsonEncoding.valueOf(encoding));
			jsonGenerator = jsonFactory.createJsonGenerator(writer);

			if (prettyPrint)
				jsonGenerator.useDefaultPrettyPrinter();

			objectMapper.writeValue(jsonGenerator, obj);
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

	protected void serializeAndLog(Object obj) {
		if (!log.isTraceEnabled())
			return;

		JsonGenerator jsonGenerator = null;
		try {
			StringWriter stringWriter = new StringWriter();
			jsonGenerator = jsonFactory.createJsonGenerator(stringWriter);
			jsonGenerator.useDefaultPrettyPrinter();
			objectMapper.writeValue(jsonGenerator, obj);
			jsonGenerator.close();
			log.debug(stringWriter.toString());
		} catch (Exception e) {
			throw new ArgeoServerException("Cannot log JSON", e);
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
