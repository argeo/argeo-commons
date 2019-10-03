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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/** {@link CsvParser} tests. */
public class CsvParserTest {
	public void testParse() throws Exception {
		String toParse = "Header1,\"Header\n2\",Header3,\"Header4\"\n" + "Col1,\"Col\n2\",Col3,\"\"\"Col4\"\"\"\n"
				+ "Col1,\"Col\n2\",Col3,\"\"\"Col4\"\"\"\n" + "Col1,\"Col\n2\",Col3,\"\"\"Col4\"\"\"\n";

		InputStream in = new ByteArrayInputStream(toParse.getBytes());

		CsvParser csvParser = new CsvParser() {
			protected void processLine(Integer lineNumber, List<String> header, List<String> tokens) {
				assert header.size() == tokens.size();
				assert 4 == tokens.size();
				assert "Col1".equals(tokens.get(0));
				assert "Col\n2".equals(tokens.get(1));
				assert "Col3".equals(tokens.get(2));
				assert "\"Col4\"".equals(tokens.get(3));
			}
		};

		csvParser.parse(in);
		in.close();
	}

}
