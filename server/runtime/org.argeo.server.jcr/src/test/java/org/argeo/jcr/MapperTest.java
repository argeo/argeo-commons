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

package org.argeo.jcr;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.Repository;

import org.apache.jackrabbit.core.TransientRepository;
import org.argeo.jcr.unit.AbstractJcrTestCase;
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
				"org/argeo/server/jcr/repository.xml");
		return res.getFile();
	}

	protected Repository createRepository() throws Exception{
		Repository repository = new TransientRepository(getRepositoryFile(), getHomeDir());
		return repository;
	}

	
}
