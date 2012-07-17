/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class CsvParserWithQuotedSeparatorTest extends TestCase {
	public void testSimpleParse() throws Exception {
		String toParse = "Header1,\"Header2\",Header3,\"Header4\"\n"
				+ "\"Col1, Col2\",\"Col\n2\",Col3,\"\"\"Col4\"\"\"\n";

		InputStream in = new ByteArrayInputStream(toParse.getBytes());

		CsvParser csvParser = new CsvParser() {
			protected void processLine(Integer lineNumber, List<String> header,
					List<String> tokens) {
				assertEquals(header.size(), tokens.size());
				assertEquals(4, tokens.size());
				assertEquals("Col1, Col2", tokens.get(0));
			}
		};
		// System.out.println(toParse);
		csvParser.parse(in);
		in.close();

	}

	public void testParseFile() throws Exception {

		final Map<Integer, Map<String, String>> lines = new HashMap<Integer, Map<String, String>>();
		InputStream in = getClass().getResourceAsStream(
				"/org/argeo/util/ReferenceFile.csv");

		CsvParserWithLinesAsMap parser = new CsvParserWithLinesAsMap() {
			protected void processLine(Integer lineNumber,
					Map<String, String> line) {
				// System.out.println("processing line #" + lineNumber);
				lines.put(lineNumber, line);
			}
		};

		parser.parse(in);
		in.close();

		Map<String, String> line = lines.get(2);
		assertEquals(",,,,", line.get("Coma testing"));
		line = lines.get(3);
		assertEquals(",, ,,", line.get("Coma testing"));
		line = lines.get(4);
		assertEquals("module1, module2", line.get("Coma testing"));
		line = lines.get(5);
		assertEquals("module1,module2", line.get("Coma testing"));
		line = lines.get(6);
		assertEquals(",module1,module2, \nmodule3, module4",
				line.get("Coma testing"));
		assertEquals(5, lines.size());

	}
}
