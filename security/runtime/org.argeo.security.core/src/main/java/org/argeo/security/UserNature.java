package org.argeo.security;

import java.io.Serializable;

public class UserNature implements Serializable {
	private static final long serialVersionUID = 1L;

	private String type;

	public String getType() {
		if (type != null)
			return type;
		else
			return getClass().getName();
	}

	public void setType(String type) {
		this.type = type;
	}
}
