package org.argeo.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.argeo.ArgeoException;

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
	 * {@link #setStrictLineAsLongAsHeader(Boolean)} is true (default) the
	 * header and the tokens are guaranteed to have the same size.
	 * 
	 * @param lineNumber
	 *            the current line number, starts at 1 (the header, if header
	 *            processing is enabled, the first lien otherwise)
	 * @param header
	 *            the read-only header or null if {@link #setNoHeader(Boolean)}
	 *            is true (default is false)
	 * @param tokens
	 *            the parse tokens
	 */
	protected abstract void processLine(Integer lineNumber,
			List<String> header, List<String> tokens);

	public synchronized void parse(InputStream in) {
		BufferedReader reader = null;
		Integer lineCount = 0;
		try {
			reader = new BufferedReader(new InputStreamReader(in));

			List<String> header = null;
			if (!noHeader) {
				String headerStr = reader.readLine();
				if (headerStr == null)// empty file
					return;
				lineCount++;
				header = new ArrayList<String>();
				StringBuffer currStr = new StringBuffer("");
				Boolean wasInquote = false;
				while (parseLine(headerStr, header, currStr, wasInquote)) {
					wasInquote = true;
				}
				header = Collections.unmodifiableList(header);
			}

			String line = null;
			lines: while ((line = reader.readLine()) != null) {
				lineCount++;
				List<String> tokens = new ArrayList<String>();
				StringBuffer currStr = new StringBuffer("");
				Boolean wasInquote = false;
				while (parseLine(line, tokens, currStr, wasInquote)) {
					line = reader.readLine();
					if (line == null)
						break;
					wasInquote = true;
				}
				if (!noHeader && strictLineAsLongAsHeader) {
					int headerSize = header.size();
					int tokenSize = tokens.size();
					if (tokenSize == 1 && line.trim().equals(""))
						continue lines;// empty line
					if (headerSize != tokenSize) {
						throw new ArgeoException("Token size " + tokenSize
								+ " is different from header size "
								+ headerSize + " at line " + lineCount
								+ ", line: " + line + ", header: " + header
								+ ", tokens: " + tokens);
					}
				}
				processLine(lineCount, header, tokens);
			}
		} catch (ArgeoException e) {
			throw e;
		} catch (IOException e) {
			throw new ArgeoException("Cannot parse CSV file (line: "
					+ lineCount + ")", e);
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (Exception e2) {
					// silent
				}
		}
	}

	/**
	 * Parses a line character by character for performance purpose
	 * 
	 * @return whether to continue parsing this line
	 */
	protected Boolean parseLine(String str, List<String> tokens,
			StringBuffer currStr, Boolean wasInquote) {
		// List<String> tokens = new ArrayList<String>();

		// System.out.println("#LINE: " + str);

		if (wasInquote)
			currStr.append('\n');

		char[] arr = str.toCharArray();
		boolean inQuote = wasInquote;
		// StringBuffer currStr = new StringBuffer("");
		for (int i = 0; i < arr.length; i++) {
			char c = arr[i];
			if (c == separator) {
				if (!inQuote) {
					tokens.add(currStr.toString());
					//System.out.println("# TOKEN: " + currStr);
					currStr.delete(0, currStr.length());
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

	public synchronized void setStrictLineAsLongAsHeader(
			Boolean strictLineAsLongAsHeader) {
		this.strictLineAsLongAsHeader = strictLineAsLongAsHeader;
	}

}
