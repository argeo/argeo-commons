package org.argeo.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Test that {@link CsvParser} can properly parse a CSV file. */
public class CsvParserParseFileTest {
	public void testParse() throws Exception {

		final Map<Integer, Map<String, String>> lines = new HashMap<Integer, Map<String, String>>();
		InputStream in = getClass().getResourceAsStream("/org/argeo/util/ReferenceFile.csv");
		CsvParserWithLinesAsMap parser = new CsvParserWithLinesAsMap() {
			protected void processLine(Integer lineNumber, Map<String, String> line) {
				lines.put(lineNumber, line);
			}
		};

		parser.parse(in);
		in.close();

		assert 5 == lines.size();
	}

}
