package org.argeo.cms.acr;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.security.auth.Subject;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.DName;
import org.argeo.api.acr.search.BasicSearch;
import org.argeo.api.acr.search.BasicSearch.Scope;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.uuid.UuidFactory;
import org.argeo.api.uuid.UuidIdentified;
import org.argeo.cms.CurrentUser;

/** Implements {@link ProvidedSession}. */
class CmsContentSession implements ProvidedSession, UuidIdentified {
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
		if (!path.startsWith(Content.ROOT_PATH))
			throw new IllegalArgumentException(path + " is not an absolute path");
		ContentProvider contentProvider = contentRepository.getMountManager().findContentProvider(path);
		String mountPath = contentProvider.getMountPath();
		String relativePath = ContentUtils.relativize(mountPath, path);
		ProvidedContent content = contentProvider.get(CmsContentSession.this, relativePath);
		return content;
	}

	@Override
	public boolean exists(String path) {
		if (!path.startsWith(Content.ROOT_PATH))
			throw new IllegalArgumentException(path + " is not an absolute path");
		ContentProvider contentProvider = contentRepository.getMountManager().findContentProvider(path);
		String mountPath = contentProvider.getMountPath();
		String relativePath = ContentUtils.relativize(mountPath, path);
		return contentProvider.exists(this, relativePath);
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
					provider.persist(s);
//					if (provider instanceof DomContentProvider) {
//						((DomContentProvider) provider).persist(s);
//					}
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
	public UUID uuid() {
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

	/*
	 * OBJECT METHODS
	 */

	@Override
	public boolean equals(Object o) {
		return UuidIdentified.equals(this, o);
	}

	@Override
	public int hashCode() {
		return UuidIdentified.hashCode(this);
	}

	@Override
	public String toString() {
		return "Content Session " + uuid + " (" + CurrentUser.getUsername(subject) + ")";
	}

	/*
	 * SEARCH
	 */
	@Override
	public Stream<Content> search(Consumer<BasicSearch> search) {
		BasicSearch s = new BasicSearch();
		search.accept(s);
		NavigableMap<String, SearchPartition> searchPartitions = new TreeMap<>();
		for (Scope scope : s.getFrom()) {
			String scopePath = scope.getUri().getPath();
			NavigableMap<String, ContentProvider> contentProviders = contentRepository.getMountManager()
					.findContentProviders(scopePath);
			for (Map.Entry<String, ContentProvider> contentProvider : contentProviders.entrySet()) {
				// TODO deal with depth
				String relPath;
				if (scopePath.startsWith(contentProvider.getKey())) {
					relPath = scopePath.substring(contentProvider.getKey().length());
				} else {
					relPath = null;
				}
				SearchPartition searchPartition = new SearchPartition(s, relPath, contentProvider.getValue());
				searchPartitions.put(contentProvider.getKey(), searchPartition);
			}
		}
		return StreamSupport.stream(new SearchPartitionsSpliterator(searchPartitions), true);
	}

	class SearchPartition {
		BasicSearch search;
		String relPath;
		ContentProvider contentProvider;

		public SearchPartition(BasicSearch search, String relPath, ContentProvider contentProvider) {
			super();
			this.search = search;
			this.relPath = relPath;
			this.contentProvider = contentProvider;
		}

		public BasicSearch getSearch() {
			return search;
		}

		public String getRelPath() {
			return relPath;
		}

		public ContentProvider getContentProvider() {
			return contentProvider;
		}

	}

	class SearchPartitionsSpliterator implements Spliterator<Content> {
		NavigableMap<String, SearchPartition> searchPartitions;

		Spliterator<Content> currentSpliterator;

		public SearchPartitionsSpliterator(NavigableMap<String, SearchPartition> searchPartitions) {
			super();
			this.searchPartitions = searchPartitions;
			SearchPartition searchPartition = searchPartitions.pollFirstEntry().getValue();
			currentSpliterator = searchPartition.getContentProvider().search(CmsContentSession.this,
					searchPartition.getSearch(), searchPartition.getRelPath());
		}

		@Override
		public boolean tryAdvance(Consumer<? super Content> action) {
			boolean remaining = currentSpliterator.tryAdvance(action);
			if (remaining)
				return true;
			if (searchPartitions.isEmpty())
				return false;
			SearchPartition searchPartition = searchPartitions.pollFirstEntry().getValue();
			currentSpliterator = searchPartition.getContentProvider().search(CmsContentSession.this,
					searchPartition.getSearch(), searchPartition.getRelPath());
			return true;
		}

		@Override
		public Spliterator<Content> trySplit() {
			if (searchPartitions.isEmpty()) {
				return null;
			} else if (searchPartitions.size() == 1) {
				NavigableMap<String, SearchPartition> newSearchPartitions = new TreeMap<>(searchPartitions);
				searchPartitions.clear();
				return new SearchPartitionsSpliterator(newSearchPartitions);
			} else {
				NavigableMap<String, SearchPartition> newSearchPartitions = new TreeMap<>();
				for (int i = 0; i < searchPartitions.size() / 2; i++) {
					Map.Entry<String, SearchPartition> searchPartition = searchPartitions.pollLastEntry();
					newSearchPartitions.put(searchPartition.getKey(), searchPartition.getValue());
				}
				return new SearchPartitionsSpliterator(newSearchPartitions);
			}
		}

		@Override
		public long estimateSize() {
			return Long.MAX_VALUE;
		}

		@Override
		public int characteristics() {
			return NONNULL;
		}

	}
}