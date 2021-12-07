package org.argeo.cms.tabular;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.argeo.api.tabular.TabularColumn;
import org.argeo.api.tabular.TabularRow;
import org.argeo.api.tabular.TabularRowIterator;
import org.argeo.api.tabular.TabularWriter;
import org.argeo.cms.ArgeoTypes;
import org.argeo.jackrabbit.unit.AbstractJackrabbitTestCase;

public class JcrTabularTest extends AbstractJackrabbitTestCase {
	private final static Log log = LogFactory.getLog(JcrTabularTest.class);

	public void testWriteReadCsv() throws Exception {
		// session().setNamespacePrefix("argeo", ArgeoNames.ARGEO_NAMESPACE);
		InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/org/argeo/cms/jcr/ldap.cnd"));
		CndImporter.registerNodeTypes(reader, session());
		reader.close();
		reader = new InputStreamReader(getClass().getResourceAsStream("/org/argeo/cms/jcr/argeo.cnd"));
		CndImporter.registerNodeTypes(reader, session());
		reader.close();
//		reader = new InputStreamReader(getClass().getResourceAsStream("/org/argeo/cms/cms.cnd"));
//		CndImporter.registerNodeTypes(reader, session());
//		reader.close();

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
