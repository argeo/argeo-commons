package org.argeo.cms.acr;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.acr.xml.DomContentProvider;

/** Implements {@link ProvidedSession}. */
class CmsContentSession implements ProvidedSession {
	final private AbstractContentRepository contentRepository;

	private Subject subject;
	private Locale locale;

	private CompletableFuture<ProvidedSession> closed = new CompletableFuture<>();

	private CompletableFuture<ContentSession> edition;

	private Set<ContentProvider> modifiedProviders = new TreeSet<>();

	public CmsContentSession(AbstractContentRepository contentRepository, Subject subject, Locale locale) {
		this.contentRepository = contentRepository;
		this.subject = subject;
		this.locale = locale;
	}

	public void close() {
		closed.complete(this);
	}

	@Override
	public CompletionStage<ProvidedSession> onClose() {
		return closed.minimalCompletionStage();
	}

	@Override
	public Content get(String path) {
		ContentProvider contentProvider = contentRepository.getMountManager().findContentProvider(path);
		String mountPath = contentProvider.getMountPath();
		String relativePath = path.substring(mountPath.length());
		if (relativePath.length() > 0 && relativePath.charAt(0) == '/')
			relativePath = relativePath.substring(1);
		ProvidedContent content = contentProvider.get(CmsContentSession.this, mountPath, relativePath);
		return content;
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
	 * NAMESPACE CONTEXT
	 */

	@Override
	public String getNamespaceURI(String prefix) {
		return NamespaceUtils.getNamespaceURI((p) -> contentRepository.getTypesManager().getPrefixes().get(p), prefix);
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		return NamespaceUtils.getPrefixes((ns) -> contentRepository.getTypesManager().getPrefixes().entrySet().stream()
				.filter(e -> e.getValue().equals(ns)).map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet()),
				namespaceURI);
	}

	@Override
	public CompletionStage<ContentSession> edit(Consumer<ContentSession> work) {
		edition = CompletableFuture.supplyAsync(() -> {
			work.accept(this);
			return this;
		}).thenApply((s) -> {
			// TODO optimise
			for (ContentProvider provider : modifiedProviders) {
				if (provider instanceof DomContentProvider) {
					((DomContentProvider) provider).persist(s);
				}
			}
			return s;
		});
		return edition.minimalCompletionStage();
	}

	@Override
	public boolean isEditing() {
		return edition != null && !edition.isDone();
	}

	@Override
	public void notifyModification(ProvidedContent content) {
		ContentProvider contentProvider = content.getProvider();
		modifiedProviders.add(contentProvider);
	}

//		@Override
//		public String findNamespace(String prefix) {
//			return prefixes.get(prefix);
//		}
//
//		@Override
//		public Set<String> findPrefixes(String namespaceURI) {
//			Set<String> res = prefixes.entrySet().stream().filter(e -> e.getValue().equals(namespaceURI))
//					.map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet());
//
//			return res;
//		}
//
//		@Override
//		public String findPrefix(String namespaceURI) {
//			if (CrName.CR_NAMESPACE_URI.equals(namespaceURI) && prefixes.containsKey(CrName.CR_DEFAULT_PREFIX))
//				return CrName.CR_DEFAULT_PREFIX;
//			return ProvidedSession.super.findPrefix(namespaceURI);
//		}

}