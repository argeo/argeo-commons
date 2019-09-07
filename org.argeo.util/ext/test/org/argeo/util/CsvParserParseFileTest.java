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
