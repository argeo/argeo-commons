/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
package org.argeo.cms.tabular;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.argeo.cms.ArgeoTypes;
import org.argeo.jackrabbit.unit.AbstractJackrabbitTestCase;
import org.argeo.node.tabular.TabularColumn;
import org.argeo.node.tabular.TabularRow;
import org.argeo.node.tabular.TabularRowIterator;
import org.argeo.node.tabular.TabularWriter;

public class JcrTabularTest extends AbstractJackrabbitTestCase {
	private final static Log log = LogFactory.getLog(JcrTabularTest.class);

	public void testWriteReadCsv() throws Exception {
		// session().setNamespacePrefix("argeo", ArgeoNames.ARGEO_NAMESPACE);
		InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/org/argeo/node/node.cnd"));
		CndImporter.registerNodeTypes(reader, session());
		reader.close();
		reader = new InputStreamReader(getClass().getResourceAsStream("/org/argeo/cms/cms.cnd"));
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
		Node tableNode = session().getRootNode().addNode("table", ArgeoTypes.ARGEO_TABLE);
		TabularWriter writer = new JcrTabularWriter(tableNode, header, ArgeoTypes.ARGEO_CSV);
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
			log.debug("Wrote tabular content " + rowCount + " rows, " + columnCount + " columns");
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
			log.debug("Read tabular content " + rowCount + " rows, " + columnCount + " columns");
	}
}
