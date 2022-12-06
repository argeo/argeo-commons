package org.argeo.cms.acr;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import javax.security.auth.Subject;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.DName;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.uuid.UuidFactory;
import org.argeo.cms.acr.xml.DomContentProvider;

/** Implements {@link ProvidedSession}. */
class CmsContentSession implements ProvidedSession {
	final private AbstractContentRepository contentRepository;

	private final UUID uuid;
	private Subject subject;
	private Locale locale;

	private UuidFactory uuidFactory;

	private CompletableFuture<ProvidedSession> closed = new CompletableFuture<>();

	private CompletableFuture<ContentSession> edition;

	private Set<ContentProvider> modifiedProviders = new HashSet<>();

	private Content sessionRunDir;

	public CmsContentSession(AbstractContentRepository contentRepository, UUID uuid, Subject subject, Locale locale,
			UuidFactory uuidFactory) {
		this.contentRepository = contentRepository;
		this.subject = subject;
		this.locale = locale;
		this.uuid = uuid;
		this.uuidFactory = uuidFactory;
	}

	public void close() {
		closed.complete(this);

		if (sessionRunDir != null)
			sessionRunDir.remove();
	}

	@Override
	public CompletionStage<ProvidedSession> onClose() {
		return closed.minimalCompletionStage();
	}

	@Override
	public Content get(String path) {
		if (!path.startsWith(ContentUtils.ROOT_SLASH))
			throw new IllegalArgumentException(path + " is not an absolute path");
		ContentProvider contentProvider = contentRepository.getMountManager().findContentProvider(path);
		String mountPath = contentProvider.getMountPath();
		String relativePath = extractRelativePath(mountPath, path);
		ProvidedContent content = contentProvider.get(CmsContentSession.this, relativePath);
		return content;
	}

	@Override
	public boolean exists(String path) {
		if (!path.startsWith(ContentUtils.ROOT_SLASH))
			throw new IllegalArgumentException(path + " is not an absolute path");
		ContentProvider contentProvider = contentRepository.getMountManager().findContentProvider(path);
		String mountPath = contentProvider.getMountPath();
		String relativePath = extractRelativePath(mountPath, path);
		return contentProvider.exists(this, relativePath);
	}

	private String extractRelativePath(String mountPath, String path) {
		String relativePath = path.substring(mountPath.length());
		if (relativePath.length() > 0 && relativePath.charAt(0) == '/')
			relativePath = relativePath.substring(1);
		return relativePath;
	}

	@Override
	public Subject getSubject() {
		return subject;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public ProvidedRepository getRepository() {
		return contentRepository;
	}

	public UuidFactory getUuidFactory() {
		return uuidFactory;
	}

	/*
	 * MOUNT MANAGEMENT
	 */
	@Override
	public Content getMountPoint(String path) {
		String[] parent = ContentUtils.getParentPath(path);
		ProvidedContent mountParent = (ProvidedContent) get(parent[0]);
//			Content mountPoint = mountParent.getProvider().get(CmsContentSession.this, null, path);
		return mountParent.getMountPoint(parent[1]);
	}

	/*
	 * EDITION
	 */
	@Override
	public CompletionStage<ContentSession> edit(Consumer<ContentSession> work) {
		edition = CompletableFuture.supplyAsync(() -> {
			work.accept(this);
			return this;
		}).thenApply((s) -> {
			synchronized (CmsContentSession.this) {
				// TODO optimise
				for (ContentProvider provider : modifiedProviders) {
					if (provider instanceof DomContentProvider) {
						((DomContentProvider) provider).persist(s);
					}
				}
				modifiedProviders.clear();
				return s;
			}
		});
		return edition.minimalCompletionStage();
	}

	@Override
	public boolean isEditing() {
		return edition != null && !edition.isDone();
	}

	@Override
	public synchronized void notifyModification(ProvidedContent content) {
		ContentProvider contentProvider = content.getProvider();
		modifiedProviders.add(contentProvider);
	}

	@Override
	public UUID getUuid() {
		return uuid;
	}

//	@Override
	public Content getSessionRunDir() {
		if (sessionRunDir == null) {
			String runDirPath = CmsContentRepository.RUN_BASE + '/' + uuid.toString();
			if (exists(runDirPath))
				sessionRunDir = get(runDirPath);
			else {
				Content runDir = get(CmsContentRepository.RUN_BASE);
				// TODO deal with no run dir available?
				sessionRunDir = runDir.add(uuid.toString(), DName.collection.qName());
			}
		}
		return sessionRunDir;
	}
}