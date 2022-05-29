package org.argeo.cms.acr;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.ContentUtils;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsSession;
import org.argeo.cms.acr.xml.DomContentProvider;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Base implementation of a {@link ProvidedRepository} integrated with a CMS.
 */
public class CmsContentRepository implements ProvidedRepository {
	private final static CmsLog log = CmsLog.getLog(CmsContentRepository.class);

	private NavigableMap<String, ContentProvider> partitions = new TreeMap<>();

	// TODO synchronize ?
	private NavigableMap<String, String> prefixes = new TreeMap<>();

//	private Schema schema;

	private CmsContentSession systemSession;

	private Map<CmsSession, CmsContentSession> userSessions = Collections.synchronizedMap(new HashMap<>());

	public CmsContentRepository() {
		prefixes.put(CrName.CR_DEFAULT_PREFIX, CrName.CR_NAMESPACE_URI);
		prefixes.put("basic", CrName.CR_NAMESPACE_URI);
		prefixes.put("owner", CrName.CR_NAMESPACE_URI);
		prefixes.put("posix", CrName.CR_NAMESPACE_URI);

		systemSession = newSystemSession();
	}

	protected CmsContentSession newSystemSession() {
		LoginContext loginContext;
		try {
			loginContext = new LoginContext(CmsAuth.DATA_ADMIN.getLoginContextName());
			loginContext.login();
		} catch (LoginException e1) {
			throw new RuntimeException("Could not login as data admin", e1);
		} finally {
		}
		return new CmsContentSession(loginContext.getSubject(), Locale.getDefault());
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
		// Subject subject = Subject.getSubject(AccessController.getContext());
		CmsSession cmsSession = CurrentUser.getCmsSession();
		CmsContentSession contentSession = userSessions.get(cmsSession);
		if (contentSession == null) {
			final CmsContentSession newContentSession = new CmsContentSession(cmsSession.getSubject(), locale);
			cmsSession.addOnCloseCallback((c) -> {
				newContentSession.close();
				userSessions.remove(cmsSession);
			});
			contentSession = newContentSession;
		}
		return contentSession;
	}

	public void addProvider(String base, ContentProvider provider) {
		partitions.put(base, provider);
		if ("/".equals(base))// root
			return;
		String[] parentPath = ContentUtils.getParentPath(base);
		Content parent = systemSession.get(parentPath[0]);
		Content mount = parent.add(parentPath[1]);
		// TODO use a boolean
		// ContentName name = new ContentName(CrName.MOUNT.getNamespaceURI(),
		// CrName.MOUNT.name(), systemSession);
		mount.put(CrName.MOUNT.get(), "true");
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
	 * FACTORIES
	 */
	public void initRootContentProvider(Path path) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setXIncludeAware(true);
			// factory.setSchema(schema);

			DocumentBuilder dBuilder = factory.newDocumentBuilder();
			dBuilder.setErrorHandler(new ErrorHandler() {

				@Override
				public void warning(SAXParseException exception) throws SAXException {
				}

				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
				}

				@Override
				public void error(SAXParseException exception) throws SAXException {
					log.error(exception);

				}
			});

			Document document;
			if (path != null && Files.exists(path)) {
				InputSource inputSource = new InputSource(path.toAbsolutePath().toUri().toString());
				inputSource.setEncoding(StandardCharsets.UTF_8.name());
				// TODO public id as well?
				document = dBuilder.parse(inputSource);
			} else {
				document = dBuilder.newDocument();
//				Element root = document.createElementNS(CrName.ROOT.getNamespaceURI(),
//						CrName.ROOT.get().toPrefixedString());
				Element root = document.createElementNS(CrName.CR_NAMESPACE_URI, CrName.ROOT.get().toPrefixedString());
				// root.setAttribute("xmlns", "");
//				root.setAttribute("xmlns:" + CrName.CR_DEFAULT_PREFIX, CrName.CR_NAMESPACE_URI);
				document.appendChild(root);

				// write it
				if (path != null) {
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					DOMSource source = new DOMSource(document);
					try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
						StreamResult result = new StreamResult(writer);
						transformer.transform(source, result);
					}
				}
			}

			DomContentProvider contentProvider = new DomContentProvider(document);
			addProvider("/", contentProvider);
		} catch (DOMException | ParserConfigurationException | SAXException | IOException
				| TransformerFactoryConfigurationError | TransformerException e) {
			throw new IllegalStateException("Cannot init ACR root " + path, e);
		}

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

		private CompletableFuture<ProvidedSession> closed = new CompletableFuture<>();

		public CmsContentSession(Subject subject, Locale locale) {
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
			Map.Entry<String, ContentProvider> entry = partitions.floorEntry(path);
			if (entry == null)
				throw new IllegalArgumentException("No entry provider found for " + path);
			String mountPath = entry.getKey();
			ContentProvider provider = entry.getValue();
			String relativePath = path.substring(mountPath.length());
			if (relativePath.length() > 0 && relativePath.charAt(0) == '/')
				relativePath = relativePath.substring(1);
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
		public String getNamespaceURI(String prefix) {
			return NamespaceUtils.getNamespaceURI((p) -> prefixes.get(p), prefix);
		}

		@Override
		public Iterator<String> getPrefixes(String namespaceURI) {
			return NamespaceUtils.getPrefixes((ns) -> prefixes.entrySet().stream().filter(e -> e.getValue().equals(ns))
					.map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet()), namespaceURI);
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

}
