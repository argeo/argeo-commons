package org.argeo.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/** Utilities to be used when Apache Commons IO is not available. */
class StreamUtils {
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	/*
	 * APACHE COMMONS IO (inspired)
	 */

	/** @return the number of bytes */
	public static Long copy(InputStream in, OutputStream out)
			throws IOException {
		Long count = 0l;
		byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
		while (true) {
			int length = in.read(buf);
			if (length < 0)
				break;
			out.write(buf, 0, length);
			count = count + length;
		}
		return count;
	}

	/** @return the number of chars */
	public static Long copy(Reader in, Writer out) throws IOException {
		Long count = 0l;
		char[] buf = new char[DEFAULT_BUFFER_SIZE];
		while (true) {
			int length = in.read(buf);
			if (length < 0)
				break;
			out.write(buf, 0, length);
			count = count + length;
		}
		return count;
	}

	public static void closeQuietly(InputStream in) {
		if (in != null)
			try {
				in.close();
			} catch (Exception e) {
				//
			}
	}

	public static void closeQuietly(OutputStream out) {
		if (out != null)
			try {
				out.close();
			} catch (Exception e) {
				//
			}
	}

	public static void closeQuietly(Reader in) {
		if (in != null)
			try {
				in.close();
			} catch (Exception e) {
				//
			}
	}

	public static void closeQuietly(Writer out) {
		if (out != null)
			try {
				out.close();
			} catch (Exception e) {
				//
			}
	}
}
