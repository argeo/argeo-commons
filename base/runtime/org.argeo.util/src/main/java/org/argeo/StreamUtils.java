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
package org.argeo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/** Utilities to be used when APache COmmons IO is not available. */
public class StreamUtils {
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

	/*
	 * APACHE COMMONS CODEC (forked)
	 */
	/**
	 * Used to build output as Hex
	 */
	private static final char[] DIGITS_LOWER = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * Used to build output as Hex
	 */
	private static final char[] DIGITS_UPPER = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * Converts an array of bytes into a String representing the hexadecimal
	 * values of each byte in order. The returned String will be double the
	 * length of the passed array, as it takes two characters to represent any
	 * given byte.
	 * 
	 * @param data
	 *            a byte[] to convert to Hex characters
	 * @return A String containing hexadecimal characters
	 * @since 1.4
	 */
	public static String encodeHexString(byte[] data) {
		return new String(encodeHex(data));
	}

	/**
	 * Converts an array of bytes into an array of characters representing the
	 * hexadecimal values of each byte in order. The returned array will be
	 * double the length of the passed array, as it takes two characters to
	 * represent any given byte.
	 * 
	 * @param data
	 *            a byte[] to convert to Hex characters
	 * @return A char[] containing hexadecimal characters
	 */
	public static char[] encodeHex(byte[] data) {
		return encodeHex(data, true);
	}

	/**
	 * Converts an array of bytes into an array of characters representing the
	 * hexadecimal values of each byte in order. The returned array will be
	 * double the length of the passed array, as it takes two characters to
	 * represent any given byte.
	 * 
	 * @param data
	 *            a byte[] to convert to Hex characters
	 * @param toLowerCase
	 *            <code>true</code> converts to lowercase, <code>false</code> to
	 *            uppercase
	 * @return A char[] containing hexadecimal characters
	 * @since 1.4
	 */
	public static char[] encodeHex(byte[] data, boolean toLowerCase) {
		return encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
	}

	/**
	 * Converts an array of bytes into an array of characters representing the
	 * hexadecimal values of each byte in order. The returned array will be
	 * double the length of the passed array, as it takes two characters to
	 * represent any given byte.
	 * 
	 * @param data
	 *            a byte[] to convert to Hex characters
	 * @param toDigits
	 *            the output alphabet
	 * @return A char[] containing hexadecimal characters
	 * @since 1.4
	 */
	protected static char[] encodeHex(byte[] data, char[] toDigits) {
		int l = data.length;
		char[] out = new char[l << 1];
		// two characters form the hex value.
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
			out[j++] = toDigits[0x0F & data[i]];
		}
		return out;
	}

}
