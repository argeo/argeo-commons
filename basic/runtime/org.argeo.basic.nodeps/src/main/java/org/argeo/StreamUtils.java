package org.argeo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Utilities to be used when APache COmmons IO is not available. */
public class StreamUtils {

	public static void copy(InputStream in, OutputStream out)
			throws IOException {
		byte[] buf = new byte[8192];
		while (true) {
			int length = in.read(buf);
			if (length < 0)
				break;
			out.write(buf, 0, length);
		}
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

	private StreamUtils() {

	}

}
