package org.argeo.security.json;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.security.ArgeoUser;
import org.argeo.security.BasicArgeoUser;
import org.argeo.security.UserNature;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.CustomDeserializerFactory;
import org.codehaus.jackson.map.deser.StdDeserializerProvider;

public class ArgeoUserMapper {
	private final static Log log = LogFactory.getLog(ArgeoUserMapper.class);

	private String userNatureTypeField = "type";

	private ObjectMapper mapper = new ObjectMapper();

	public ArgeoUserMapper() {
		CustomDeserializerFactory dsf = new CustomDeserializerFactory();
		dsf.addSpecificMapping(UserNature.class, new UserNatureDeserializer());
		StdDeserializerProvider sdp = new StdDeserializerProvider(dsf);
		mapper.setDeserializerProvider(sdp);
	}

	public ArgeoUser parse(String content) throws JsonMappingException,
			JsonParseException, IOException {

		return mapper.readValue(content, BasicArgeoUser.class);
	}

	private class UserNatureDeserializer extends JsonDeserializer<UserNature> {
		private JsonFactory jsonFactory = new JsonFactory();

		@Override
		public UserNature deserialize(JsonParser parser,
				DeserializationContext dc) throws IOException,
				JsonProcessingException {
			try {
				// first read as Json DOM in order to extract the type
				// TODO: optimize with streaming API
				JsonNode root = parser.readValueAsTree();
				String type = root.get(userNatureTypeField).getTextValue();

				// Write it back as a string
				StringWriter writer = new StringWriter();
				JsonGenerator generator = jsonFactory
						.createJsonGenerator(writer);
				generator.setCodec(mapper);
				generator.writeTree(root);
				String str = writer.toString();

				log.info("type=" + type + ", str=" + str);

				// TODO: use context classloader (in OSGi)
				// TODO: externalize type/classes mapping
				Class<UserNature> clss = (Class<UserNature>) Class
						.forName(type);
				UserNature result = mapper.readValue(str, clss);

				// JavaType javaType = TypeFactory.fromClass(clss);
				// BeanDeserializer bd = new BeanDeserializer(javaType);
				// JsonParser parser2 = jsonFactory.createJsonParser(str);

				return result;
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Cannot deserialize", e);
			}
		}

	}
}
