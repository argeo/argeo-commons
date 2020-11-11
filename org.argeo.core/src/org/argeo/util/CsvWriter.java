package org.argeo.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
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
	 * @param out the stream to write to. Caller is responsible for closing it.
	 * 
	 * @deprecated Use {@link #CsvWriter(OutputStream, Charset)} instead.
	 * 
	 */
	@Deprecated
	public CsvWriter(OutputStream out) {
		this.out = new OutputStreamWriter(out);
	}

	/**
	 * Creates a CSV writer.
	 * 
	 * @param out      the stream to write to. Caller is responsible for closing it.
	 * @param encoding the encoding to use.
	 * 
	 * @deprecated Use {@link #CsvWriter(OutputStream, Charset)} instead.
	 */
	@Deprecated
	public CsvWriter(OutputStream out, String encoding) {
		try {
			this.out = new OutputStreamWriter(out, encoding);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Creates a CSV writer.
	 * 
	 * @param out     the stream to write to. Caller is responsible for closing it.
	 * @param charset the charset to use
	 */
	public CsvWriter(OutputStream out, Charset charset) {
		this.out = new OutputStreamWriter(out, charset);
	}

	/**
	 * Creates a CSV writer.
	 * 
	 * @param out     the stream to write to. Caller is responsible for closing it.
	 */
	public CsvWriter(Writer writer) {
		this.out = writer;
	}

	/**
	 * Write a CSV line. Also used to write a header if needed (this is transparent
	 * for the CSV writer): simply call it first, before writing the lines.
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
			throw new RuntimeException("Could not write " + tokens, e);
		}
	}

	/**
	 * Write a CSV line. Also used to write a header if needed (this is transparent
	 * for the CSV writer): simply call it first, before writing the lines.
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
			throw new RuntimeException("Could not write " + tokens, e);
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
