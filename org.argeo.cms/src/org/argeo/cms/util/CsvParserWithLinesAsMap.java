package org.argeo.cms.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV parser allowing to process lines as maps whose keys are the header
 * fields.
 */
public abstract class CsvParserWithLinesAsMap extends CsvParser {

	/**
	 * Actually processes a line.
	 * 
	 * @param lineNumber the current line number, starts at 1 (the header, if header
	 *                   processing is enabled, the first lien otherwise)
	 * @param line       the parsed tokens as a map whose keys are the header fields
	 */
	protected abstract void processLine(Integer lineNumber, Map<String, String> line);

	protected final void processLine(Integer lineNumber, List<String> header, List<String> tokens) {
		if (header == null)
			throw new IllegalArgumentException("Only CSV with header is supported");
		Map<String, String> line = new HashMap<String, String>();
		for (int i = 0; i < header.size(); i++) {
			String key = header.get(i);
			String value = null;
			if (i < tokens.size())
				value = tokens.get(i);
			line.put(key, value);
		}
		processLine(lineNumber, line);
	}

}
