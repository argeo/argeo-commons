package org.argeo.server.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.server.Serializer;
import org.argeo.server.ServerSerializer;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerFactory;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.StdSerializerProvider;

public class JsonServerSerializer implements ServerSerializer, Serializer {
	private final static Log log = LogFactory
			.getLog(JsonServerSerializer.class);

	private JsonFactory jsonFactory = new JsonFactory();
	private ObjectMapper objectMapper = new ObjectMapper();
	private SerializerProvider serializerProvider = new CustomSerializerProvider();

	private Boolean prettyPrint = false;

	private Boolean asHtml = false;

	private String contentTypeCharset = "UTF-8";

	// private Map<Class<?>,String> ignoredFields = new HashMap<Class<?>,
	// String>();

	public JsonServerSerializer() {
		objectMapper.setSerializerProvider(serializerProvider);
	}

	public void serialize(Object obj, HttpServletRequest request,
			HttpServletResponse response) {
		if (asHtml)
			response.setContentType("text/html;charset=" + contentTypeCharset);
		else
			response.setContentType("application/json;charset="
					+ contentTypeCharset);

		try {
			if (asHtml)
				response.getWriter().append("<pre>");

			serialize(response.getWriter(), obj);

			if (asHtml)
				response.getWriter().append("</pre>");

		} catch (IOException e) {
			throw new ArgeoException("Cannot open response stream.", e);
		}
	}

	public void serialize(Writer writer, Object obj) {
		serializeAndLog(obj);

		JsonGenerator jsonGenerator = null;
		try {
			jsonGenerator = jsonFactory.createJsonGenerator(writer);

			if (prettyPrint)
				jsonGenerator.useDefaultPrettyPrinter();

			objectMapper.writeValue(jsonGenerator, obj);
			jsonGenerator.flush();
		} catch (Exception e) {
			throw new ArgeoException("Cannot serialize " + obj, e);
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
			throw new ArgeoException("Cannot log JSON", e);
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

	public void setContentTypeCharset(String contentTypeCharset) {
		this.contentTypeCharset = contentTypeCharset;
	}

	static class CustomSerializerProvider extends StdSerializerProvider {

		public CustomSerializerProvider() {
			super();
		}

		public CustomSerializerProvider(SerializationConfig config,
				StdSerializerProvider src, SerializerFactory f) {
			super(config, src, f);
		}

		protected StdSerializerProvider createInstance(
				SerializationConfig config, SerializerFactory jsf) {
			return new CustomSerializerProvider(config, this, jsf);
		}

		@Override
		public JsonSerializer<Object> getUnknownTypeSerializer(
				Class<?> unknownType) {
			JsonSerializer<Object> res = new JsonSerializer<Object>() {
				public void serialize(Object value, JsonGenerator jgen,
						SerializerProvider provider)
						throws JsonMappingException {
					if (log.isDebugEnabled())
						log.warn("Unknown serializer for "
								+ value.getClass().getName());
					try {
						jgen.writeNull();
					} catch (Exception e) {
						throw new ArgeoException("Cannot write null", e);
					}
				}

			};

			return res;
		}

	}
}
