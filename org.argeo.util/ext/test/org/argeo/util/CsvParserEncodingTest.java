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

/** Tests that {@link CsvParser} can deal properly with encodings. */
public class CsvParserEncodingTest {

	private String iso = "ISO-8859-1";
	private String utf8 = "UTF-8";

	public void testParse() throws Exception {

		String xml = new String("áéíóúñ,éééé");
		byte[] utfBytes = xml.getBytes(utf8);
		byte[] isoBytes = xml.getBytes(iso);

		InputStream inUtf = new ByteArrayInputStream(utfBytes);
		InputStream inIso = new ByteArrayInputStream(isoBytes);

		CsvParser csvParser = new CsvParser() {
			protected void processLine(Integer lineNumber, List<String> header, List<String> tokens) {
				assert header.size() == tokens.size();
				assert 2 == tokens.size();
				assert "áéíóúñ".equals(tokens.get(0));
				assert "éééé".equals(tokens.get(1));
			}
		};

		csvParser.parse(inUtf, utf8);
		inUtf.close();
		csvParser.parse(inIso, iso);
		inIso.close();
	}
}
