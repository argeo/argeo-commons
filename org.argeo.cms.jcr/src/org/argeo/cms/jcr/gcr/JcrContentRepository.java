package org.argeo.cms.jcr.gcr;

import java.security.AccessController;
import java.util.Locale;

import javax.jcr.Repository;
import javax.security.auth.Subject;

import org.argeo.api.gcr.ContentRepository;
import org.argeo.api.gcr.ContentSession;

public class JcrContentRepository implements ContentRepository {
	private Repository jcrRepository;

	@Override
	public ContentSession get() {
		// TODO retrieve locale from Subject?
		return get(Locale.getDefault());
	}

	@Override
	public ContentSession get(Locale locale) {
		Subject subject = Subject.getSubject(AccessController.getContext());
		return new JcrContentSession(jcrRepository, subject, locale);
	}

	public Repository getJcrRepository() {
		return jcrRepository;
	}

	public void setJcrRepository(Repository jcrRepository) {
		this.jcrRepository = jcrRepository;
	}

}
