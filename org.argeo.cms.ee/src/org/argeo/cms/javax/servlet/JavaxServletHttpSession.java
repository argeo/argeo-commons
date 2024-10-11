package org.argeo.cms.javax.servlet;

import javax.servlet.http.HttpSession;

import org.argeo.cms.auth.RemoteAuthSession;

public class JavaxServletHttpSession implements RemoteAuthSession {
	private HttpSession session;

	public JavaxServletHttpSession(HttpSession session) {
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
