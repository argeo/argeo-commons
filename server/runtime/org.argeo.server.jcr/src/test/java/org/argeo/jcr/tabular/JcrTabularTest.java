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

package org.argeo.jcr.tabular;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.TransientRepository;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.unit.AbstractJcrTestCase;
import org.argeo.util.tabular.TabularColumn;
import org.argeo.util.tabular.TabularRow;
import org.argeo.util.tabular.TabularRowIterator;
import org.argeo.util.tabular.TabularWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class JcrTabularTest extends AbstractJcrTestCase {
	private final static Log log = LogFactory.getLog(JcrTabularTest.class);

	public void testWriteReadCsv() throws Exception {
		session().setNamespacePrefix("argeo", ArgeoNames.ARGEO_NAMESPACE);
		InputStreamReader reader = new InputStreamReader(getClass()
				.getResourceAsStream("/org/argeo/jcr/argeo.cnd"));
		CndImporter.registerNodeTypes(reader, session());
		reader.close();

		// write
		Integer columnCount = 15;
		Long rowCount = 1000l;
		String stringValue = "test, \ntest";

		List<TabularColumn> header = new ArrayList<TabularColumn>();
		for (int i = 0; i < columnCount; i++) {
			header.add(new TabularColumn("col" + i, PropertyType.STRING));
		}
		Node tableNode = session().getRootNode().addNode("table",
				ArgeoTypes.ARGEO_TABLE);
		TabularWriter writer = new JcrTabularWriter(tableNode, header,
				ArgeoTypes.ARGEO_CSV);
		for (int i = 0; i < rowCount; i++) {
			List<Object> objs = new ArrayList<Object>();
			for (int j = 0; j < columnCount; j++) {
				objs.add(stringValue);
			}
			writer.appendRow(objs.toArray());
		}
		writer.close();
		session().save();

		if (log.isDebugEnabled())
			log.debug("Wrote tabular content " + rowCount + " rows, "
					+ columnCount + " columns");
		// read
		TabularRowIterator rowIt = new JcrTabularRowIterator(tableNode);
		Long count = 0l;
		while (rowIt.hasNext()) {
			TabularRow tr = rowIt.next();
			assertEquals(header.size(), tr.size());
			count++;
		}
		assertEquals(rowCount, count);
		if (log.isDebugEnabled())
			log.debug("Read tabular content " + rowCount + " rows, "
					+ columnCount + " columns");
	}

	protected File getRepositoryFile() throws Exception {
		Resource res = new ClassPathResource(
				"org/argeo/server/jcr/repository-h2.xml");
		return res.getFile();
	}

	protected Repository createRepository() throws Exception {
		// JackrabbitContainer repo = new JackrabbitContainer();
		// repo.setHomeDirectory(getHomeDir());
		// repo.setConfiguration(new FileSystemResource(
		// getRepositoryFile()));
		// repo.setInMemory(true);
		// repo.set
		Repository repository = new TransientRepository(getRepositoryFile(),
				getHomeDir());
		return repository;
	}

}
