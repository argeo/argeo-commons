package org.argeo.cms.gcr;

import java.security.AccessController;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.security.auth.Subject;

import org.argeo.api.gcr.Content;
import org.argeo.api.gcr.ContentRepository;
import org.argeo.api.gcr.ContentSession;
import org.argeo.api.gcr.spi.ContentProvider;
import org.argeo.cms.internal.runtime.CmsContextImpl;

public class CmsContentRepository implements ContentRepository {
	private NavigableMap<String, ContentProvider> partitions = new TreeMap<>();

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

	class CmsContentSession implements ContentSession {
		private Subject subject;
		private Locale locale;

		public CmsContentSession(Subject subject, Locale locale) {
			this.subject = subject;
			this.locale = locale;
		}

		@Override
		public Content get(String path) {
			Map.Entry<String, ContentProvider> provider = partitions.floorEntry(path);
			String relativePath = path.substring(provider.getKey().length());
			return provider.getValue().get(relativePath);
		}

		@Override
		public Subject getSubject() {
			return subject;
		}

		@Override
		public Locale getLocale() {
			return locale;
		}

	}

}
