package org.argeo.security.json;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.security.ArgeoUser;
import org.argeo.security.SimpleArgeoUser;
import org.argeo.security.UserNature;
import org.argeo.security.core.ArgeoUserDetails;
import org.argeo.security.nature.CoworkerNature;
import org.argeo.security.nature.SimpleUserNature;
import org.argeo.server.json.JsonServerMapper;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;

import com.springsource.json.writer.JSONArray;
import com.springsource.json.writer.JSONObject;

public class ArgeoUserJsonTest extends TestCase {
	private static Log log = LogFactory.getLog(ArgeoUserJsonTest.class);

	public void testMapper() throws Exception {
		List<UserNature> natures = new ArrayList<UserNature>();

		SimpleUserNature sun = new SimpleUserNature();
		sun.setFirstName("Mickey");
		sun.setEmail("username@domain.com");
		natures.add(sun);

		CoworkerNature cwn = new CoworkerNature();
		cwn.setMobile("+123456789");
		natures.add(cwn);

		GrantedAuthority[] roles = { new GrantedAuthorityImpl("ROLE1"),
				new GrantedAuthorityImpl("ROLE2") };
		ArgeoUserDetails argeoUserDetails = new ArgeoUserDetails("USER",
				natures, "PASSWORD", roles);

		SimpleArgeoUser argeoUser = new SimpleArgeoUser(argeoUserDetails);

		StringWriter writer = new StringWriter();

		JsonFactory jsonFactory = new JsonFactory();
		JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
		jsonGenerator.useDefaultPrettyPrinter();

		ObjectMapper objectMapper = new ObjectMapper();

		objectMapper.writeValue(jsonGenerator, argeoUser);
		String audJo = writer.toString();

		log.info("audJo:\n" + audJo);

		// BasicArgeoUser aud = objectMapper.readValue(audJo,
		// BasicArgeoUser.class);

		JsonServerMapper mapper = JsonServerMapperTest.createJsonServerMapper();
		ArgeoUser aud = (ArgeoUser) mapper.deserialize(audJo);

		assertEquals(argeoUser.getUsername(), aud.getUsername());
		assertEquals(argeoUser.getRoles().size(), aud.getRoles().size());
		assertEquals(argeoUser.getUserNatures().size(), aud.getUserNatures()
				.size());

		assertSimpleUserNature((SimpleUserNature) argeoUser.getUserNatures()
				.get(0), (SimpleUserNature) aud.getUserNatures().get(0));
		assertCoworkerNature(
				(CoworkerNature) argeoUser.getUserNatures().get(1),
				(CoworkerNature) aud.getUserNatures().get(1));

	}

	public static void assertSimpleUserNature(SimpleUserNature expected,
			SimpleUserNature reached) {
		assertEquals(expected.getEmail(), reached.getEmail());
	}

	public static void assertCoworkerNature(CoworkerNature expected,
			CoworkerNature reached) {
		assertEquals(expected.getMobile(), reached.getMobile());
	}

	public void testSeriDeserialize() {
		List<UserNature> natures = new ArrayList<UserNature>();
		JSONArray naturesJo = new JSONArray();

		SimpleUserNature sun = new SimpleUserNature();
		sun.setEmail("username@domain.com");
		natures.add(sun);
		naturesJo.put(new JSONObject(sun));

		CoworkerNature cwn = new CoworkerNature();
		cwn.setMobile("+123456789");
		natures.add(cwn);
		naturesJo.put(new JSONObject(cwn));

		GrantedAuthority[] roles = { new GrantedAuthorityImpl("ROLE1"),
				new GrantedAuthorityImpl("ROLE1") };
		ArgeoUserDetails argeoUserDetails = new ArgeoUserDetails("USER",
				natures, "PASSWORD", roles);

		JSONObject argeoUserDetailsJo = new JSONObject(argeoUserDetails);
		argeoUserDetailsJo.put("userNatures", naturesJo);

		log.info("argeoUserDetailsJo=" + argeoUserDetailsJo.toString(2));

		// JSONParser jsonParser = new JSONParser();
		// ArgeoUserDetails argeoUserDetails = JSONParser
	}
}
