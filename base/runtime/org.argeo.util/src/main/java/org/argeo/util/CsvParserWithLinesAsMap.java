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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.argeo.ArgeoException;

/**
 * CSV parser allowing to process lines as maps whose keys are the header
 * fields.
 */
public abstract class CsvParserWithLinesAsMap extends CsvParser {

	/**
	 * Actually processes a line.
	 * 
	 * @param lineNumber
	 *            the current line number, starts at 1 (the header, if header
	 *            processing is enabled, the first lien otherwise)
	 * @param line
	 *            the parsed tokens as a map whose keys are the header fields
	 */
	protected abstract void processLine(Integer lineNumber,
			Map<String, String> line);

	protected final void processLine(Integer lineNumber, List<String> header,
			List<String> tokens) {
		if (header == null)
			throw new ArgeoException("Only CSV with header is supported");
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
