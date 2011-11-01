package org.argeo.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

public class CsvParserEncodingTestCase extends TestCase {

	private String iso = "ISO-8859-1";
	private String utf8 = "UTF-8";

	public void testParse() throws Exception {

		String xml = new String("áéíóúñ,éééé");
		byte[] utfBytes = xml.getBytes(utf8);
		byte[] isoBytes = xml.getBytes(iso);

		InputStream inUtf = new ByteArrayInputStream(utfBytes);
		InputStream inIso = new ByteArrayInputStream(isoBytes);

		CsvParser csvParser = new CsvParser() {
			protected void processLine(Integer lineNumber, List<String> header,
					List<String> tokens) {
				assertEquals(header.size(), tokens.size());
				assertEquals(2, tokens.size());
				assertEquals("áéíóúñ", tokens.get(0));
				assertEquals("éééé", tokens.get(1));
			}
		};

		csvParser.parse(inUtf, utf8);
		inUtf.close();
		csvParser.parse(inIso, iso);
		inIso.close();
	}
}
