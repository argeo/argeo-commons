package org.argeo.jcr;

import java.io.File;

import javax.jcr.Node;

import org.argeo.server.jackrabbit.unit.AbstractJcrTestCase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

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

		Node node = bnm.save(session(), mySo);
		session().save();
		JcrUtils.debug(node);
	}

	protected File getRepositoryFile() throws Exception {
		Resource res = new ClassPathResource(
				"org/argeo/server/jcr/repository-inMemory.xml");
		return res.getFile();
	}

}
