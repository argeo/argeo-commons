package org.argeo.cms.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses a CSV file interpreting the first line as a header. The
 * {@link #parse(InputStream)} method and the setters are synchronized so that
 * the object cannot be modified when parsing.
 */
public abstract class CsvParser {
	private char separator = ',';
	private char quote = '\"';

	private Boolean noHeader = false;
	private Boolean strictLineAsLongAsHeader = true;

	/**
	 * Actually process a parsed line. If
	 * {@link #setStrictLineAsLongAsHeader(Boolean)} is true (default) the header
	 * and the tokens are guaranteed to have the same size.
	 * 
	 * @param lineNumber the current line number, starts at 1 (the header, if header
	 *                   processing is enabled, the first line otherwise)
	 * @param header     the read-only header or null if
	 *                   {@link #setNoHeader(Boolean)} is true (default is false)
	 * @param tokens     the parsed tokens
	 */
	protected abstract void processLine(Integer lineNumber, List<String> header, List<String> tokens);

	/**
	 * Parses the CSV file (stream is closed at the end)
	 * 
	 * @param in the stream to parse
	 * 
	 * @deprecated Use {@link #parse(InputStream, Charset)} instead.
	 */
	@Deprecated
	public synchronized void parse(InputStream in) {
		parse(in, (Charset) null);
	}

	/**
	 * Parses the CSV file (stream is closed at the end)
	 * 
	 * @param in       the stream to parse
	 * @param encoding the encoding to use.
	 * 
	 * @deprecated Use {@link #parse(InputStream, Charset)} instead.
	 */
	@Deprecated
	public synchronized void parse(InputStream in, String encoding) {
		Reader reader;
		if (encoding == null)
			reader = new InputStreamReader(in);
		else
			try {
				reader = new InputStreamReader(in, encoding);
			} catch (UnsupportedEncodingException e) {
				throw new IllegalArgumentException(e);
			}
		parse(reader);
	}

	/**
	 * Parses the CSV file (stream is closed at the end)
	 * 
	 * @param in      the stream to parse
	 * @param charset the charset to use
	 */
	public synchronized void parse(InputStream in, Charset charset) {
		Reader reader;
		if (charset == null)
			reader = new InputStreamReader(in);
		else
			reader = new InputStreamReader(in, charset);
		parse(reader);
	}

	/**
	 * Parses the CSV file (stream is closed at the end)
	 * 
	 * @param reader the reader to use (it will be buffered)
	 */
	public synchronized void parse(Reader reader) {
		Integer lineCount = 0;
		try (BufferedReader bufferedReader = new BufferedReader(reader)) {
			List<String> header = null;
			if (!noHeader) {
				String headerStr = bufferedReader.readLine();
				if (headerStr == null)// empty file
					return;
				lineCount++;
				header = new ArrayList<String>();
				StringBuffer currStr = new StringBuffer("");
				Boolean wasInquote = false;
				while (parseLine(headerStr, header, currStr, wasInquote)) {
					headerStr = bufferedReader.readLine();
					if (headerStr == null)
						break;
					wasInquote = true;
				}
				header = Collections.unmodifiableList(header);
			}

			String line = null;
			lines: while ((line = bufferedReader.readLine()) != null) {
				line = preProcessLine(line);
				if (line == null) {
					// skip line
					continue lines;
				}
				lineCount++;
				List<String> tokens = new ArrayList<String>();
				StringBuffer currStr = new StringBuffer("");
				Boolean wasInquote = false;
				sublines: while (parseLine(line, tokens, currStr, wasInquote)) {
					line = bufferedReader.readLine();
					if (line == null)
						break sublines;
					wasInquote = true;
				}
				if (!noHeader && strictLineAsLongAsHeader) {
					int headerSize = header.size();
					int tokenSize = tokens.size();
					if (tokenSize == 1 && line.trim().equals(""))
						continue lines;// empty line
					if (headerSize != tokenSize) {
						throw new IllegalStateException("Token size " + tokenSize + " is different from header size "
								+ headerSize + " at line " + lineCount + ", line: " + line + ", header: " + header
								+ ", tokens: " + tokens);
					}
				}
				processLine(lineCount, header, tokens);
			}
		} catch (IOException e) {
			throw new RuntimeException("Cannot parse CSV file (line: " + lineCount + ")", e);
		}
	}

	/**
	 * Called before each (logical) line is processed, giving a change to modify it
	 * (typically for cleaning dirty files). To be overridden, return the line
	 * unchanged by default. Skip the line if 'null' is returned.
	 */
	protected String preProcessLine(String line) {
		return line;
	}

	/**
	 * Parses a line character by character for performance purpose
	 * 
	 * @return whether to continue parsing this line
	 */
	protected Boolean parseLine(String str, List<String> tokens, StringBuffer currStr, Boolean wasInquote) {
		if (wasInquote)
			currStr.append('\n');

		char[] arr = str.toCharArray();
		boolean inQuote = wasInquote;
		for (int i = 0; i < arr.length; i++) {
			char c = arr[i];
			if (c == separator) {
				if (!inQuote) {
					tokens.add(currStr.toString());
//					currStr.delete(0, currStr.length());
					currStr.setLength(0);
					currStr.trimToSize();
				} else {
					// we don't remove separator that are in a quoted substring
					// System.out
					// .println("IN QUOTE, got a separator: [" + c + "]");
					currStr.append(c);
				}
			} else if (c == quote) {
				if (inQuote && (i + 1) < arr.length && arr[i + 1] == quote) {
					// case of double quote
					currStr.append(quote);
					i++;
				} else {// standard
					inQuote = inQuote ? false : true;
				}
			} else {
				currStr.append(c);
			}
		}

		if (!inQuote) {
			tokens.add(currStr.toString());
			// System.out.println("# TOKEN: " + currStr);
		}
		// if (inQuote)
		// throw new ArgeoException("Missing quote at the end of the line "
		// + str + " (parsed: " + tokens + ")");
		if (inQuote)
			return true;
		else
			return false;
		// return tokens;
	}

	public char getSeparator() {
		return separator;
	}

	public synchronized void setSeparator(char separator) {
		this.separator = separator;
	}

	public char getQuote() {
		return quote;
	}

	public synchronized void setQuote(char quote) {
		this.quote = quote;
	}

	public Boolean getNoHeader() {
		return noHeader;
	}

	public synchronized void setNoHeader(Boolean noHeader) {
		this.noHeader = noHeader;
	}

	public Boolean getStrictLineAsLongAsHeader() {
		return strictLineAsLongAsHeader;
	}

	public synchronized void setStrictLineAsLongAsHeader(Boolean strictLineAsLongAsHeader) {
		this.strictLineAsLongAsHeader = strictLineAsLongAsHeader;
	}

}
