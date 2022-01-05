package org.argeo.cms.servlet;

import org.argeo.cms.auth.HttpSession;

public class ServletHttpSession implements HttpSession {
	private javax.servlet.http.HttpSession session;

	public ServletHttpSession(javax.servlet.http.HttpSession session) {
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
