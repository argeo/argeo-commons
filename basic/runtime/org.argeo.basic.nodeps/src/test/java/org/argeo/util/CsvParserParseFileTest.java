package org.argeo.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class CsvParserParseFileTest extends TestCase {
	public void testParse() throws Exception {

		final Map<Integer, Map<String, String>> lines = new HashMap<Integer, Map<String, String>>();
		InputStream in = getClass().getResourceAsStream(
				"/org/argeo/util/ReferenceFile.csv");
		CsvParserWithLinesAsMap parser = new CsvParserWithLinesAsMap() {
			protected void processLine(Integer lineNumber,
					Map<String, String> line) {
				lines.put(lineNumber, line);
			}
		};

		parser.parse(in);
		in.close();

		assertEquals(5, lines.size());
	}

}
