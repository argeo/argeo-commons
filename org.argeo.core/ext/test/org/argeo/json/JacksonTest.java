package org.argeo.json;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonTest {

	@Test
	public void testSimpleObjectMapping() throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper();
		Map<String, Integer> map = new HashMap<>();
		map.put("one", 1);
		map.put("two", 2);
		String s = om.writeValueAsString(map);
		System.out.println(s);
	}
}
