package org.argeo.jcr;

import javax.jcr.Node;

import org.argeo.server.jackrabbit.unit.AbstractJcrTestCase;

public class MapperTest extends AbstractJcrTestCase {
	public void testSimpleObject() throws Exception {
		SimpleObject mySo = new SimpleObject();
		mySo.setInteger(100);
		mySo.setString("hello world");

		OtherObject oo1 = new OtherObject();
		oo1.setKey("someKey");
		oo1.setValue("stringValue");
		mySo.setOtherObject(oo1);

		OtherObject oo2 = new OtherObject();
		oo2.setKey("anotherSimpleObject");
		oo2.setValue(new SimpleObject());
		mySo.setAnotherObject(oo2);

		BeanNodeMapper bnm = new BeanNodeMapper();

		Node node = bnm.saveOrUpdate(session(), mySo);
		session().save();
		JcrUtils.debug(node);
	}
}
