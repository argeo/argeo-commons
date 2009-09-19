package org.argeo.server.json;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.ArgeoServerException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.ObjectCodec;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;

public class GenericJsonDeserializer<T> extends JsonDeserializer<T> {
	private final static Log log = LogFactory
			.getLog(GenericJsonDeserializer.class);

	private JsonFactory jsonFactory = new JsonFactory();
	private ObjectCodec objectCodec = new ObjectMapper();

	private String typeField = "type";

	private List<JsonObjectFactory> objectFactories = new ArrayList<JsonObjectFactory>();

	@SuppressWarnings("unchecked")
	@Override
	public T deserialize(JsonParser parser, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		// first read as Json DOM in order to extract the type
		// TODO: optimize with streaming API
		JsonNode root = parser.readValueAsTree();
		String type = root.get(typeField).getTextValue();

		// Write it back as a string
		StringWriter writer = new StringWriter();
		JsonGenerator generator = jsonFactory.createJsonGenerator(writer);
		generator.setCodec(objectCodec);
		generator.writeTree(root);
		String str = writer.toString();

		if (log.isTraceEnabled())
			log.debug("Deserialize object of type=" + type + ", str=" + str);

		JsonObjectFactory objectFactory = null;
		jofs: for (JsonObjectFactory jof : objectFactories) {
			if (jof.supports(type)) {
				objectFactory = jof;
				break jofs;
			}
		}

		if (objectFactory == null)
			throw new ArgeoServerException(
					"Cannot find JSON object factory for type " + type);

		return (T)objectFactory.readValue(type, str);
	}

	public void setTypeField(String typeField) {
		this.typeField = typeField;
	}

	public void setObjectFactories(List<JsonObjectFactory> objectFactories) {
		this.objectFactories = objectFactories;
	}

	public List<JsonObjectFactory> getObjectFactories() {
		return objectFactories;
	}

}
