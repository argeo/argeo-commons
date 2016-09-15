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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

/** Write in CSV format. */
public class CsvWriter {
	private final Writer out;

	private char separator = ',';
	private char quote = '\"';

	/**
	 * Creates a CSV writer.
	 * 
	 * @param out
	 *            the stream to write to. Caller is responsible for closing it.
	 */
	public CsvWriter(OutputStream out) {
		this.out = new OutputStreamWriter(out);
	}

	/**
	 * Creates a CSV writer.
	 * 
	 * @param out
	 *            the stream to write to. Caller is responsible for closing it.
	 */
	public CsvWriter(OutputStream out, String encoding) {
		try {
			this.out = new OutputStreamWriter(out, encoding);
		} catch (UnsupportedEncodingException e) {
			throw new UtilsException("Cannot initialize CSV writer", e);
		}
	}

	/**
	 * Write a CSV line. Also used to write a header if needed (this is
	 * transparent for the CSV writer): simply call it first, before writing the
	 * lines.
	 */
	public void writeLine(List<?> tokens) {
		try {
			Iterator<?> it = tokens.iterator();
			while (it.hasNext()) {
				writeToken(it.next().toString());
				if (it.hasNext())
					out.write(separator);
			}
			out.write('\n');
			out.flush();
		} catch (IOException e) {
			throw new UtilsException("Could not write " + tokens, e);
		}
	}

	/**
	 * Write a CSV line. Also used to write a header if needed (this is
	 * transparent for the CSV writer): simply call it first, before writing the
	 * lines.
	 */
	public void writeLine(Object[] tokens) {
		try {
			for (int i = 0; i < tokens.length; i++) {
				if (tokens[i] == null) {
					// TODO configure how to deal with null
					writeToken("");
				} else {
					writeToken(tokens[i].toString());
				}
				if (i != (tokens.length - 1))
					out.write(separator);
			}
			out.write('\n');
			out.flush();
		} catch (IOException e) {
			throw new UtilsException("Could not write " + tokens, e);
		}
	}

	protected void writeToken(String token) throws IOException {
		// +2 for possible quotes, another +2 assuming there would be an already
		// quoted string where quotes needs to be duplicated
		// another +2 for safety
		// we don't want to increase buffer size while writing
		StringBuffer buf = new StringBuffer(token.length() + 6);
		char[] arr = token.toCharArray();
		boolean shouldQuote = false;
		for (char c : arr) {
			if (!shouldQuote) {
				if (c == separator)
					shouldQuote = true;
				if (c == '\n')
					shouldQuote = true;
			}

			if (c == quote) {
				shouldQuote = true;
				// duplicate quote
				buf.append(quote);
			}

			// generic case
			buf.append(c);
		}

		if (shouldQuote == true)
			out.write(quote);
		out.write(buf.toString());
		if (shouldQuote == true)
			out.write(quote);
	}

	public void setSeparator(char separator) {
		this.separator = separator;
	}

	public void setQuote(char quote) {
		this.quote = quote;
	}

}
