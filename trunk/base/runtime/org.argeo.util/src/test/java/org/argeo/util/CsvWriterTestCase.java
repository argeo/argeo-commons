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
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class CsvWriterTestCase extends TestCase {
	public void testWrite() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		final CsvWriter csvWriter = new CsvWriter(out);

		String[] header = { "Header1", "Header 2", "Header,3", "Header\n4",
				"Header\"5\"" };
		String[] line1 = { "Value1", "Value 2", "Value,3", "Value\n4",
				"Value\"5\"" };
		csvWriter.writeLine(Arrays.asList(header));
		csvWriter.writeLine(Arrays.asList(line1));

		String reference = "Header1,Header 2,\"Header,3\",\"Header\n4\",\"Header\"\"5\"\"\"\n"
				+ "Value1,Value 2,\"Value,3\",\"Value\n4\",\"Value\"\"5\"\"\"\n";
		String written = new String(out.toByteArray());
		assertEquals(reference, written);
		out.close();
		System.out.println(written);

		final List<String> allTokens = new ArrayList<String>();
		CsvParser csvParser = new CsvParser() {
			protected void processLine(Integer lineNumber, List<String> header,
					List<String> tokens) {
				if (lineNumber == 2)
					allTokens.addAll(header);
				allTokens.addAll(tokens);
			}
		};
		ByteArrayInputStream in = new ByteArrayInputStream(written.getBytes());
		csvParser.parse(in);
		in.close();
		List<String> allTokensRef = new ArrayList<String>();
		allTokensRef.addAll(Arrays.asList(header));
		allTokensRef.addAll(Arrays.asList(line1));

		assertEquals(allTokensRef.size(), allTokens.size());
		for (int i = 0; i < allTokensRef.size(); i++)
			assertEquals(allTokensRef.get(i), allTokens.get(i));
	}

}
