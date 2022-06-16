package org.argeo.cms.acr;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.cms.acr.fs.FsContentProvider;
import org.argeo.util.naming.LdapAttrs;

/**
 * A standalone {@link ProvidedRepository} with a single {@link Subject} (which
 * also provides the system session).
 */
public class SingleUserContentRepository extends AbstractContentRepository {
	private final Subject subject;
	private final Locale locale;

	private UUID uuid;

	// the single session
	private CmsContentSession contentSession;

	public SingleUserContentRepository(Subject subject) {
		this(subject, Locale.getDefault());
	}

	public SingleUserContentRepository(Subject subject, Locale locale) {
		Objects.requireNonNull(subject);
		Objects.requireNonNull(locale);

		this.subject = subject;
		this.locale = locale;

		// TODO use an UUID factory
		this.uuid = UUID.randomUUID();
	}

	@Override
	public void start() {
		Objects.requireNonNull(subject);
		Objects.requireNonNull(locale);

		super.start();
		initRootContentProvider(null);
		if (contentSession != null)
			throw new IllegalStateException("Repository is already started, stop it first.");
		contentSession = new CmsContentSession(this, uuid, subject, locale);
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
		return new CmsContentSession(this, uuid, subject, Locale.getDefault());
	}

	public static void main(String... args) {
		Path homePath = Paths.get(System.getProperty("user.home"));
		String username = System.getProperty("user.name");
		X500Principal principal = new X500Principal(LdapAttrs.uid + "=" + username + ",dc=localhost");
		Subject subject = new Subject();
		subject.getPrincipals().add(principal);

		SingleUserContentRepository contentRepository = new SingleUserContentRepository(subject);
		contentRepository.start();
		FsContentProvider homeContentProvider = new FsContentProvider("/home", homePath);
		contentRepository.addProvider(homeContentProvider);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> contentRepository.stop(), "Shutdown content repository"));

		ContentSession contentSession = contentRepository.get();
		ContentUtils.traverse(contentSession.get("/"), (c, depth) -> ContentUtils.print(c, System.out, depth, false),
				2);

	}
}
