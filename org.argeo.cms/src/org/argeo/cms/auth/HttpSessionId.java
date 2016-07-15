package org.argeo.cms.auth;

import org.argeo.cms.CmsException;

public class HttpSessionId {
	private final String value;

	public HttpSessionId(String value) {
		if (value == null)
			throw new CmsException("value cannot be null");
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof HttpSessionId && ((HttpSessionId) obj).getValue().equals(value);
	}

	@Override
	public String toString() {
		return "HttpSessionId #" + value;
	}

}
