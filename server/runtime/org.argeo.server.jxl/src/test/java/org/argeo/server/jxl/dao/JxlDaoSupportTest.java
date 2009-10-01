package org.argeo.server.jxl.dao;

import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

public class JxlDaoSupportTest extends TestCase {
	public void testBasic() throws Exception {
		JxlDaoSupport jsd = new JxlDaoSupport();
		jsd.getExternalRefs().put("test", new OtherObject());

		InputStream in = null;
		try {
			in = getClass().getResourceAsStream("/dao/simple.xls");
			jsd.load(in);

			SimpleObject soAaa = jsd.getByKey(SimpleObject.class, "aaa");
			assertNotNull(soAaa);
			assertEquals("aaa", soAaa.getString());
			assertEquals(1, soAaa.getInteger().intValue());
			assertNotNull(soAaa.getOtherObject());
			assertEquals("USD", soAaa.getOtherObject().getKey());
			assertEquals("US Dollar", soAaa.getOtherObject().getValue());

			SimpleObject soBbb = jsd.getByKey(SimpleObject.class, "bbb");
			assertNotNull(soBbb.getOtherObject());
			assertEquals("bbb", ((SimpleObject) soBbb.getOtherObject()
					.getValue()).getString());

			List<SimpleObject> simpleObjects = jsd.list(SimpleObject.class,
					null);
			assertEquals(2, simpleObjects.size());
		} finally {
			if (in != null)
				in.close();
		}

	}
}
