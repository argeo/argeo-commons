package org.argeo.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

public class CsvParserTestCase extends TestCase {
	public void testParse() throws Exception {
		String toParse = "Header1,\"Header2\",Header3,\"Header4\"\n"
				+ "Col1,\"Col2\",Col3,\"\"\"Col4\"\"\"\n"
				+ "Col1,\"Col2\",Col3,\"\"\"Col4\"\"\"\n"
				+ "Col1,\"Col2\",Col3,\"\"\"Col4\"\"\"\n";

		InputStream in = new ByteArrayInputStream(toParse.getBytes());

		CsvParser csvParser = new CsvParser() {
			protected void processLine(Integer lineNumber, List<String> header,
					List<String> tokens) {
				assertEquals(header.size(), tokens.size());
				assertEquals(4, tokens.size());
				assertEquals("Col1", tokens.get(0));
				assertEquals("Col2", tokens.get(1));
				assertEquals("Col3", tokens.get(2));
				assertEquals("\"Col4\"", tokens.get(3));
			}
		};

		csvParser.parse(in);
		in.close();
	}
}
