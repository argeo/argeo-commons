package org.argeo.cms.http.server;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/** A part in a multi-part message. */
public class MimePart {
//		public HttpPartType type;
	String contentType;
	String name;
	String submittedFileName;
//		public String value;
	byte[] bytes;

	public String getContentType() {
		return contentType;
	}

	public String getName() {
		return name;
	}

	public String getSubmittedFileName() {
		return submittedFileName;
	}

	public InputStream getInputStream() {
		return new ByteArrayInputStream(bytes);
	}
}
