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

package org.argeo.server.jxl.dao;

import java.util.List;

import junit.framework.TestCase;

import org.springframework.core.io.ClassPathResource;

@SuppressWarnings("restriction")
public class JxlDaoSupportTest extends TestCase {
	public void testBasic() throws Exception {
		JxlDaoSupport jsd = new JxlDaoSupport();
		jsd.getExternalRefs().put("test", new OtherObject());

		jsd.getResources().add(new ClassPathResource("/dao/simple.xls"));
		jsd.init();

		SimpleObject soAaa = jsd.getByKey(SimpleObject.class, "aaa");
		assertNotNull(soAaa);
		assertEquals("aaa", soAaa.getString());
		assertEquals(1, soAaa.getInteger().intValue());
		assertNotNull(soAaa.getOtherObject());
		assertEquals("USD", soAaa.getOtherObject().getKey());
		assertEquals("US Dollar", soAaa.getOtherObject().getValue());

		SimpleObject soBbb = jsd.getByKey(SimpleObject.class, "bbb");
		assertNotNull(soBbb.getOtherObject());
		assertEquals("bbb", ((SimpleObject) soBbb.getOtherObject().getValue())
				.getString());

		List<SimpleObject> simpleObjects = jsd.list(SimpleObject.class, null);
		assertEquals(4, simpleObjects.size());

		List<CollectionsObject> collectionsObjects = jsd.list(
				CollectionsObject.class, null);
		assertEquals(4, collectionsObjects.size());
	}
}
