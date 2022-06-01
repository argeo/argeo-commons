package org.argeo.cms.acr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Validator;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.ContentUtils;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsSession;
import org.argeo.cms.acr.xml.DomContentProvider;
import org.argeo.cms.acr.xml.DomUtils;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Base implementation of a {@link ProvidedRepository} integrated with a CMS.
 */
public class CmsContentRepository implements ProvidedRepository {
	private final static CmsLog log = CmsLog.getLog(CmsContentRepository.class);

	private NavigableMap<String, ContentProvider> partitions = new TreeMap<>();

	// TODO synchronize ?
//	private NavigableMap<String, String> prefixes = new TreeMap<>();

//	private Schema schema;
	private ContentTypesManager contentTypesManager;

	private CmsContentSession systemSession;

	private Map<CmsSession, CmsContentSession> userSessions = Collections.synchronizedMap(new HashMap<>());

	// utilities
	private TransformerFactory transformerFactory = TransformerFactory.newInstance();

	public final static String ACR_MOUNT_PATH_PROPERTY = "acr.mount.path";

	public CmsContentRepository() {
		contentTypesManager = new ContentTypesManager();
		contentTypesManager.init();
		Set<QName> types = contentTypesManager.listTypes();
		for (QName type : types) {
			log.debug(type);
		}

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

	public void registerTypes(String prefix, String namespaceURI, String schemaSystemId) {
		contentTypesManager.registerTypes(prefix, namespaceURI, schemaSystemId);
//		String registeredUri = prefixes.get(prefix);
//		if (registeredUri == null) {
//			prefixes.put(prefix, namespaceURI);
//			return;
//		}
//		if (!registeredUri.equals(namespaceURI))
//			throw new IllegalStateException("Prefix " + prefix + " is already registred for " + registeredUri);
//		// do nothing if same namespace is already registered
	}

	/*
	 * FACTORIES
	 */
	public void initRootContentProvider(Path path) {
		try {
//			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//			factory.setNamespaceAware(true);
//			factory.setXIncludeAware(true);
//			factory.setSchema(contentTypesManager.getSchema());
//
			DocumentBuilder dBuilder = contentTypesManager.newDocumentBuilder();

			Document document;
//			if (path != null && Files.exists(path)) {
//				InputSource inputSource = new InputSource(path.toAbsolutePath().toUri().toString());
//				inputSource.setEncoding(StandardCharsets.UTF_8.name());
//				// TODO public id as well?
//				document = dBuilder.parse(inputSource);
//			} else {
			document = dBuilder.newDocument();
			Element root = document.createElementNS(CrName.CR_NAMESPACE_URI, CrName.ROOT.get().toPrefixedString());

			for (String prefix : contentTypesManager.getPrefixes().keySet()) {
//				root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix,
//						contentTypesManager.getPrefixes().get(prefix));
				DomUtils.addNamespace(root, prefix, contentTypesManager.getPrefixes().get(prefix));
			}

			document.appendChild(root);

			// write it
			if (path != null) {
				try (OutputStream out = Files.newOutputStream(path)) {
					writeDom(document, out);
				}
			}
//			}

			DomContentProvider contentProvider = new DomContentProvider(null, document);
			addProvider("/", contentProvider);
		} catch (DOMException | IOException e) {
			throw new IllegalStateException("Cannot init ACR root " + path, e);
		}

	}

	public void writeDom(Document document, OutputStream out) throws IOException {
		try {
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			DOMSource source = new DOMSource(document);
			contentTypesManager.validate(source);
			StreamResult result = new StreamResult(out);
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new IOException("Cannot write dom", e);
		}
	}

	/*
	 * MOUNT MANAGEMENT
	 */

	@Override
	public ContentProvider getMountContentProvider(Content mountPoint, boolean initialize, QName... types) {
		String mountPath = mountPoint.getPath();
		if (partitions.containsKey(mountPath))
			// TODO check consistency with types
			return partitions.get(mountPath);
		DocumentBuilder dBuilder = contentTypesManager.newDocumentBuilder();
		Document document;
		if (initialize) {
			QName firstType = types[0];
			document = dBuilder.newDocument();
			String prefix = ((ProvidedContent) mountPoint).getSession().getPrefix(firstType.getNamespaceURI());
			Element root = document.createElementNS(firstType.getNamespaceURI(),
					prefix + ":" + firstType.getLocalPart());
			DomUtils.addNamespace(root, prefix, firstType.getNamespaceURI());
			document.appendChild(root);
//			try (OutputStream out = mountPoint.open(OutputStream.class)) {
//				writeDom(document, out);
//			} catch (IOException e) {
//				throw new IllegalStateException("Cannot write mount from " + mountPoint, e);
//			}
		} else {
			try (InputStream in = mountPoint.open(InputStream.class)) {
				document = dBuilder.parse(in);
				// TODO check consistency with types
			} catch (IOException | SAXException e) {
				throw new IllegalStateException("Cannot load mount from " + mountPoint, e);
			}
		}
		DomContentProvider contentProvider = new DomContentProvider(mountPath, document);
		partitions.put(mountPath, contentProvider);
		return contentProvider;
	}

	@Override
	public boolean shouldMount(QName... types) {
		if (types.length == 0)
			throw new IllegalArgumentException("Types must be provided");
		QName firstType = types[0];
		Set<QName> registeredTypes = contentTypesManager.listTypes();
		if (registeredTypes.contains(firstType))
			return true;
		return false;
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

		private CompletableFuture<ContentSession> edition;

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
			return NamespaceUtils.getNamespaceURI((p) -> contentTypesManager.getPrefixes().get(p), prefix);
		}

		@Override
		public Iterator<String> getPrefixes(String namespaceURI) {
			return NamespaceUtils.getPrefixes(
					(ns) -> contentTypesManager.getPrefixes().entrySet().stream().filter(e -> e.getValue().equals(ns))
							.map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet()),
					namespaceURI);
		}

		@Override
		public CompletionStage<ContentSession> edit(Consumer<ContentSession> work) {
			edition = CompletableFuture.supplyAsync(() -> {
				work.accept(this);
				return this;
			}).thenApply((s) -> {
				// TODO optimise
				for (ContentProvider provider : partitions.values()) {
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
