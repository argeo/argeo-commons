package org.argeo.cms.acr;

import java.security.AccessController;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.internal.runtime.CmsContextImpl;

public class CmsContentRepository implements ProvidedRepository {
	private NavigableMap<String, ContentProvider> partitions = new TreeMap<>();

	// TODO synchronize ?
	private NavigableMap<String, String> prefixes = new TreeMap<>();

	public CmsContentRepository() {
		prefixes.put(CrName.CR_DEFAULT_PREFIX, CrName.CR_NAMESPACE_URI);
		prefixes.put("basic", CrName.CR_NAMESPACE_URI);
		prefixes.put("owner", CrName.CR_NAMESPACE_URI);
		prefixes.put("posix", CrName.CR_NAMESPACE_URI);
	}

	public void start() {

	}

	public void stop() {

	}

	/*
	 * REPOSITORY
	 */

	@Override
	public ContentSession get() {
		return get(CmsContextImpl.getCmsContext().getDefaultLocale());
	}

	@Override
	public ContentSession get(Locale locale) {
		Subject subject = Subject.getSubject(AccessController.getContext());
		return new CmsContentSession(subject, locale);
	}

	public void addProvider(String base, ContentProvider provider) {
		partitions.put(base, provider);
	}

	public void registerPrefix(String prefix, String namespaceURI) {
		String registeredUri = prefixes.get(prefix);
		if (registeredUri == null) {
			prefixes.put(prefix, namespaceURI);
			return;
		}
		if (!registeredUri.equals(namespaceURI))
			throw new IllegalStateException("Prefix " + prefix + " is already registred for " + registeredUri);
		// do nothing if same namespace is already registered
	}

	/*
	 * NAMESPACE CONTEXT
	 */

	/*
	 * SESSION
	 */

	class CmsContentSession implements ProvidedSession {
		private Subject subject;
		private Locale locale;

		public CmsContentSession(Subject subject, Locale locale) {
			this.subject = subject;
			this.locale = locale;
		}

		@Override
		public Content get(String path) {
			Map.Entry<String, ContentProvider> entry = partitions.floorEntry(path);
			String mountPath = entry.getKey();
			ContentProvider provider = entry.getValue();
			String relativePath = path.substring(mountPath.length());
			return provider.get(CmsContentSession.this, mountPath, relativePath);
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
			return CmsContentRepository.this;
		}

		/*
		 * NAMESPACE CONTEXT
		 */

		@Override
		public String findNamespace(String prefix) {
			return prefixes.get(prefix);
		}

		@Override
		public Set<String> findPrefixes(String namespaceURI) {
			Set<String> res = prefixes.entrySet().stream().filter(e -> e.getValue().equals(namespaceURI))
					.map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet());

			return res;
		}

		@Override
		public String findPrefix(String namespaceURI) {
			if (CrName.CR_NAMESPACE_URI.equals(namespaceURI) && prefixes.containsKey(CrName.CR_DEFAULT_PREFIX))
				return CrName.CR_DEFAULT_PREFIX;
			return ProvidedSession.super.findPrefix(namespaceURI);
		}

	}

}
