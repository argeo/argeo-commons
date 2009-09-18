package org.argeo.security.json;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.argeo.security.ArgeoUser;
import org.argeo.security.BasicArgeoUser;
import org.argeo.security.UserNature;
import org.argeo.server.json.GenericJsonDeserializer;
import org.argeo.server.json.JsonObjectFactoryImpl;
import org.argeo.server.json.JsonServerMapper;

public class JsonServerMapperTest extends TestCase {
	public void testDeserialize() throws Exception {
		JsonServerMapper mapper = createJsonServerMapper();

		Reader reader = null;
		try {
			InputStream in = getClass().getResource(
					"/org/argeo/security/json/gandalf2.json").openStream();
			reader = new InputStreamReader(in);

			ArgeoUser user = (ArgeoUser) mapper.deserialize(reader);
			assertEquals("gandalf2", user.getUsername());
			assertEquals(2, user.getRoles().size());
			assertEquals(2, user.getUserNatures().size());
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	@SuppressWarnings("unchecked")
	public static JsonServerMapper createJsonServerMapper() throws Exception {
		JsonServerMapper mapper = new JsonServerMapper();
		mapper.setTargetClass(BasicArgeoUser.class);
		GenericJsonDeserializer jsonDeserializer = new GenericJsonDeserializer();
		jsonDeserializer.getObjectFactories().add(new JsonObjectFactoryImpl());
		mapper.getDeserializers().put(UserNature.class, jsonDeserializer);
		mapper.afterPropertiesSet();
		return mapper;
	}
}
