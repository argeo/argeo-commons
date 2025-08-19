package org.argeo.cms.jakarta.servlet;

import org.argeo.cms.auth.RemoteAuthSession;

public class ServletHttpSession implements RemoteAuthSession {
	private jakarta.servlet.http.HttpSession session;

	public ServletHttpSession(jakarta.servlet.http.HttpSession session) {
		super();
		this.session = session;
	}

	@Override
	public boolean isValid() {
		try {// test http session
			session.getCreationTime();
			return true;
		} catch (IllegalStateException ise) {
			return false;
		}
	}

	@Override
	public String getId() {
		return session.getId();
	}

}
