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
