package org.argeo.server.json;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.ArgeoServerException;
import org.argeo.server.ServerDeserializer;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.DeserializationProblemHandler;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.deser.CustomDeserializerFactory;
import org.codehaus.jackson.map.deser.StdDeserializerProvider;
import org.springframework.beans.factory.InitializingBean;

public class JsonServerMapper extends JsonServerSerializer implements
		ServerDeserializer, InitializingBean {
	private final static Log log = LogFactory.getLog(JsonServerMapper.class);

	private Class targetClass;

	private Map<Class, JsonDeserializer> deserializers = new HashMap<Class, JsonDeserializer>();

	public void afterPropertiesSet() throws Exception {
		CustomDeserializerFactory dsf = new CustomDeserializerFactory();
		for (Class clss : deserializers.keySet()) {
			dsf.addSpecificMapping(clss, deserializers.get(clss));
		}
		StdDeserializerProvider sdp = new StdDeserializerProvider(dsf);
		getObjectMapper().setDeserializerProvider(sdp);
		// ignore unkown properties
		getObjectMapper().getDeserializationConfig().addHandler(
				new DeserializationProblemHandler() {
					public boolean handleUnknownProperty(
							DeserializationContext ctxt,
							JsonDeserializer<?> deserializer, Object bean,
							String propertyName) throws IOException,
							JsonProcessingException {
						if (log.isDebugEnabled())
							log.debug("Ignore property " + propertyName);
						return true;
					}
				});
	}

	public Object deserialize(Reader reader) {
		try {
			// String body = request.getParameter("body");
			// if (body == null) {
			// // lets read the message body instead
			// BufferedReader reader = request.getReader();
			// StringBuffer buffer = new StringBuffer();
			// String line = null;
			// while (((line = reader.readLine()) != null)) {
			// buffer.append(line);
			// }
			// body = buffer.toString();
			// }
			return getObjectMapper().readValue(reader, targetClass);
		} catch (Exception e) {
			throw new ArgeoServerException("Cannot deserialize " + reader, e);
		}

	}

	public void setTargetClass(Class targetClass) {
		this.targetClass = targetClass;
	}

	public void setDeserializers(Map<Class, JsonDeserializer> deserializers) {
		this.deserializers = deserializers;
	}

}
