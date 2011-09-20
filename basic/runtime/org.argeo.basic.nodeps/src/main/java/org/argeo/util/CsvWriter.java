package org.argeo.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.argeo.ArgeoException;

/** Write in CSV format. */
public class CsvWriter {
	private final PrintWriter out;

	private char separator = ',';
	private char quote = '\"';

	/**
	 * Creates a CSV writer. The header will be written immediately to the
	 * stream.
	 * 
	 * @param out
	 *            the stream to write to. Caller is responsible for closing it.
	 */
	public CsvWriter(OutputStream out) {
		super();
		this.out = new PrintWriter(out);
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
					out.print(separator);
			}
			out.print('\n');
			out.flush();
		} catch (IOException e) {
			throw new ArgeoException("Could not write " + tokens, e);
		}
	}

	protected void writeToken(String token) throws IOException {
		// +2 for possible quotes, another +2 assuming there would be an already
		// quoted string where quotes needs to be duplicated
		// another +2 for safety
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
			out.print(quote);
		out.print(buf.toString());
		if (shouldQuote == true)
			out.print(quote);
	}
}
