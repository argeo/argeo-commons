package org.argeo.cms.http.server;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

/** Initial implementation. API will change. */
class FormDataExtractor {

	static List<MimePart> extractParts(HttpExchange httpExchange) {
		Headers headers = httpExchange.getRequestHeaders();
		String contentType = headers.getFirst("Content-Type");
		if (contentType.startsWith("multipart/form-data")) {
			// found form data
			String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
			// as of rfc7578 - prepend "\r\n--"
			byte[] boundaryBytes = ("\r\n--" + boundary).getBytes(UTF_8);
			byte[] payload = getInputAsBinary(httpExchange.getRequestBody());
			ArrayList<MimePart> list = new ArrayList<>();

			List<Integer> offsets = searchBytes(payload, boundaryBytes, 0, payload.length - 1);
			for (int idx = 0; idx < offsets.size(); idx++) {
				int startPart = offsets.get(idx);
				int endPart = payload.length;
				if (idx < offsets.size() - 1) {
					endPart = offsets.get(idx + 1);
				}
				byte[] part = Arrays.copyOfRange(payload, startPart, endPart);
				// look for header
				int headerEnd = indexOf(part, "\r\n\r\n".getBytes(UTF_8), 0, part.length - 1);
				if (headerEnd > 0) {
					MimePart p = new MimePart();
					byte[] head = Arrays.copyOfRange(part, 0, headerEnd);
					String header = new String(head);
					// extract name from header
					int nameIndex = header.indexOf("\r\nContent-Disposition: form-data; name=");
					if (nameIndex >= 0) {
						int startMarker = nameIndex + 39;
						// check for extra filename field
						int fileNameStart = header.indexOf("; filename=");
						if (fileNameStart >= 0) {
							String filename = header.substring(fileNameStart + 11,
									header.indexOf("\r\n", fileNameStart));
							p.submittedFileName = filename.replace('"', ' ').replace('\'', ' ').trim();
							p.name = header.substring(startMarker, fileNameStart).replace('"', ' ').replace('\'', ' ')
									.trim();
//							p.type = HttpPartType.FILE;
						} else {
							int endMarker = header.indexOf("\r\n", startMarker);
							if (endMarker == -1)
								endMarker = header.length();
							p.name = header.substring(startMarker, endMarker).replace('"', ' ').replace('\'', ' ')
									.trim();
//							p.type = HttpPartType.TEXT;
						}
					} else {
						// skip entry if no name is found
						continue;
					}
					// extract content type from header
					int typeIndex = header.indexOf("\r\nContent-Type:");
					if (typeIndex >= 0) {
						int startMarker = typeIndex + 15;
						int endMarker = header.indexOf("\r\n", startMarker);
						if (endMarker == -1)
							endMarker = header.length();
						p.contentType = header.substring(startMarker, endMarker).trim();
					}

					// handle content
//					if (p.type == HttpPartType.TEXT) {
//						// extract text value
//						byte[] body = Arrays.copyOfRange(part, headerEnd + 4, part.length);
//						p.value = new String(body);
//					} else {
//						// must be a file upload
					p.bytes = Arrays.copyOfRange(part, headerEnd + 4, part.length);
//					}
					list.add(p);
				}
			}
			return list;
		} else {
			return new ArrayList<>();
		}
	}

	private static byte[] getInputAsBinary(InputStream requestStream) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			byte[] buf = new byte[100000];
			int bytesRead = 0;
			while ((bytesRead = requestStream.read(buf)) != -1) {
				// while (requestStream.available() > 0) {
				// int i = requestStream.read(buf);
				bos.write(buf, 0, bytesRead);
			}
			requestStream.close();
			bos.close();
		} catch (IOException e) {
//			Logger log = Logger.getLogger(MultiPartFormDataExtractor.class.getName());
//			log.log(Level.SEVERE, "error while decoding http input stream", e);
			throw new UncheckedIOException(e);
		}
		return bos.toByteArray();
	}

	/**
	 * Search bytes in byte array returns indexes within this byte-array of all
	 * occurrences of the specified(search bytes) byte array in the specified range
	 */
	private static List<Integer> searchBytes(byte[] srcBytes, byte[] searchBytes, int searchStartIndex,
			int searchEndIndex) {
		final int destSize = searchBytes.length;
		final List<Integer> positionIndexList = new ArrayList<Integer>();
		int cursor = searchStartIndex;
		while (cursor < searchEndIndex + 1) {
			int index = indexOf(srcBytes, searchBytes, cursor, searchEndIndex);
			if (index >= 0) {
				positionIndexList.add(index);
				cursor = index + destSize;
			} else {
				cursor++;
			}
		}
		return positionIndexList;
	}

	/**
	 * Returns the index within this byte-array of the first occurrence of the
	 * specified(search bytes) byte array.<br>
	 * Starting the search at the specified index, and end at the specified index.
	 */
	private static int indexOf(byte[] srcBytes, byte[] searchBytes, int startIndex, int endIndex) {
		if (searchBytes.length == 0 || (endIndex - startIndex + 1) < searchBytes.length) {
			return -1;
		}
		int maxScanStartPosIdx = srcBytes.length - searchBytes.length;
		final int loopEndIdx;
		if (endIndex < maxScanStartPosIdx) {
			loopEndIdx = endIndex;
		} else {
			loopEndIdx = maxScanStartPosIdx;
		}
		int lastScanIdx = -1;
		label: // goto label
		for (int i = startIndex; i <= loopEndIdx; i++) {
			for (int j = 0; j < searchBytes.length; j++) {
				if (srcBytes[i + j] != searchBytes[j]) {
					continue label;
				}
				lastScanIdx = i + j;
			}
			if (endIndex < lastScanIdx || lastScanIdx - i + 1 < searchBytes.length) {
				// it becomes more than the last index
				// or less than the number of search bytes
				return -1;
			}
			return i;
		}
		return -1;
	}

//	public enum HttpPartType {
//		TEXT, FILE
//	}
}
