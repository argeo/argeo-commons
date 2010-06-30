/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.argeo.security.json;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.argeo.security.ArgeoUser;
import org.argeo.security.SimpleArgeoUser;
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
		mapper.setTargetClass(SimpleArgeoUser.class);
		GenericJsonDeserializer jsonDeserializer = new GenericJsonDeserializer();
		jsonDeserializer.getObjectFactories().add(new JsonObjectFactoryImpl());
		mapper.getDeserializers().put(UserNature.class, jsonDeserializer);
		mapper.afterPropertiesSet();
		return mapper;
	}
}
