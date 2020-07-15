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
