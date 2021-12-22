package org.argeo.cms.jcr.gcr;

import java.security.PrivilegedAction;
import java.util.Locale;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;

import org.argeo.api.gcr.ContentSession;
import org.argeo.jcr.JcrException;

public class JcrContentSession implements ContentSession {
	private Repository jcrRepository;
	private Subject subject;
	private Locale locale;
	private Session jcrSession;

	protected JcrContentSession(Repository jcrRepository, Subject subject, Locale locale) {
		this.jcrRepository = jcrRepository;
		this.subject = subject;
		this.locale = locale;
		this.jcrSession = Subject.doAs(this.subject, (PrivilegedAction<Session>) () -> {
			try {
				return jcrRepository.login();
			} catch (RepositoryException e) {
				throw new JcrException("Cannot log in to repository", e);
			}
		});
	}

	@Override
	public Subject getSubject() {
		return subject;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	public Session getJcrSession() {
		return jcrSession;
	}

}
