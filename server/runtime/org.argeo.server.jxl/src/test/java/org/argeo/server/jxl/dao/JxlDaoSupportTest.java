package org.argeo.server.jxl.dao;

import java.util.List;

import junit.framework.TestCase;

import org.springframework.core.io.ClassPathResource;

public class JxlDaoSupportTest extends TestCase {
	public void testBasic() throws Exception {
		JxlDaoSupport jsd = new JxlDaoSupport();
		jsd.getExternalRefs().put("test", new OtherObject());

		jsd.getWorkbooks().add(new ClassPathResource("/dao/simple.xls"));
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
		assertEquals(3, collectionsObjects.size());
	}
}
