package org.argeo.cms.acr;

import java.util.Locale;
import java.util.Objects;

import javax.security.auth.Subject;

import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.spi.ProvidedRepository;

/**
 * A standalone {@link ProvidedRepository} with a single {@link Subject} (which
 * also provides the system session).
 */
public class SingleUserContentRepository extends AbstractContentRepository {
	private final Subject subject;
	private final Locale locale;

	// the single session
	private CmsContentSession contentSession;

	public SingleUserContentRepository(Subject subject) {
		this(subject, Locale.getDefault());

		initRootContentProvider(null);
	}

	public SingleUserContentRepository(Subject subject, Locale locale) {
		Objects.requireNonNull(subject);
		Objects.requireNonNull(locale);

		this.subject = subject;
		this.locale = locale;
	}

	@Override
	public void start() {
		Objects.requireNonNull(subject);
		Objects.requireNonNull(locale);

		super.start();
		if (contentSession != null)
			throw new IllegalStateException("Repository is already started, stop it first.");
		contentSession = new CmsContentSession(this, subject, locale);
	}

	@Override
	public void stop() {
		if (contentSession != null)
			contentSession.close();
		contentSession = null;
		super.stop();
	}

	@Override
	public ContentSession get(Locale locale) {
		if (!this.locale.equals(locale))
			throw new UnsupportedOperationException("This repository does not support multi-locale sessions");
		return contentSession;
	}

	@Override
	public ContentSession get() {
		return contentSession;
	}

	@Override
	protected CmsContentSession newSystemSession() {
		return new CmsContentSession(this, subject, Locale.getDefault());
	}

}
