package org.argeo.cms.acr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.RuntimeNamespaceContext;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.acr.xml.DomContentProvider;
import org.argeo.cms.acr.xml.DomUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Base implementation of a {@link ProvidedRepository}.
 */
public abstract class AbstractContentRepository implements ProvidedRepository {
	private final static CmsLog log = CmsLog.getLog(AbstractContentRepository.class);

	private MountManager mountManager;
	private TypesManager typesManager;

	private CmsContentSession systemSession;

	private Set<ContentProvider> providersToAdd = new HashSet<>();

	// utilities
	/** Should be used only to copy source and results. */
	private TransformerFactory identityTransformerFactory = TransformerFactory.newInstance();

	public final static String ACR_MOUNT_PATH_PROPERTY = "acr.mount.path";

	public AbstractContentRepository() {
		// types
		typesManager = new TypesManager();
		typesManager.init();
		Set<QName> types = typesManager.listTypes();
		if (log.isTraceEnabled())
			for (QName type : types) {
				log.trace(type + " - " + typesManager.getAttributeTypes(type));
			}

	}

	protected abstract CmsContentSession newSystemSession();

	public void start() {
		systemSession = newSystemSession();
		// mounts
		mountManager = new MountManager(systemSession);
	}

	public void stop() {
		systemSession.close();
		systemSession = null;
	}

	/*
	 * REPOSITORY
	 */

	public void addProvider(ContentProvider provider) {
		if (mountManager == null)
			providersToAdd.add(provider);
		else
			mountManager.addStructuralContentProvider(provider);
	}

	public void registerTypes(String prefix, String namespaceURI, String schemaSystemId) {
		typesManager.registerTypes(prefix, namespaceURI, schemaSystemId);
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
			DocumentBuilder dBuilder = typesManager.newDocumentBuilder();

			Document document;
//			if (path != null && Files.exists(path)) {
//				InputSource inputSource = new InputSource(path.toAbsolutePath().toUri().toString());
//				inputSource.setEncoding(StandardCharsets.UTF_8.name());
//				// TODO public id as well?
//				document = dBuilder.parse(inputSource);
//			} else {
			document = dBuilder.newDocument();
			Element root = document.createElementNS(CrName.CR_NAMESPACE_URI, CrName.ROOT.get().toPrefixedString());

			for (String prefix : RuntimeNamespaceContext.getPrefixes().keySet()) {
//				root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix,
//						contentTypesManager.getPrefixes().get(prefix));
				DomUtils.addNamespace(root, prefix,
						RuntimeNamespaceContext.getNamespaceContext().getNamespaceURI(prefix));
			}

			document.appendChild(root);

			// write it
			if (path != null) {
				try (OutputStream out = Files.newOutputStream(path)) {
					writeDom(document, out);
				}
			}
//			}

			String mountPath = "/";
			DomContentProvider contentProvider = new DomContentProvider(mountPath, document);
			addProvider(contentProvider);
		} catch (DOMException | IOException e) {
			throw new IllegalStateException("Cannot init ACR root " + path, e);
		}

		// add content providers already notified
		for (ContentProvider contentProvider : providersToAdd)
			addProvider(contentProvider);
		providersToAdd.clear();
	}

	public void writeDom(Document document, OutputStream out) throws IOException {
		try {
			Transformer transformer = identityTransformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			DOMSource source = new DOMSource(document);
			typesManager.validate(source);
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
		// TODO check consistency with types

		return mountManager.getOrAddMountedProvider(mountPath, (path) -> {
			DocumentBuilder dBuilder = typesManager.newDocumentBuilder();
			Document document;
			if (initialize) {
				QName firstType = types[0];
				document = dBuilder.newDocument();
				String prefix = ((ProvidedContent) mountPoint).getSession().getPrefix(firstType.getNamespaceURI());
				Element root = document.createElementNS(firstType.getNamespaceURI(),
						prefix + ":" + firstType.getLocalPart());
				DomUtils.addNamespace(root, prefix, firstType.getNamespaceURI());
				document.appendChild(root);
			} else {
				try (InputStream in = mountPoint.open(InputStream.class)) {
					document = dBuilder.parse(in);
					// TODO check consistency with types
				} catch (IOException | SAXException e) {
					throw new IllegalStateException("Cannot load mount from " + mountPoint, e);
				}
			}
			DomContentProvider contentProvider = new DomContentProvider(path, document);
			return contentProvider;
		});
	}

	@Override
	public boolean shouldMount(QName... types) {
		if (types.length == 0)
			return false;
		QName firstType = types[0];
		Set<QName> registeredTypes = typesManager.listTypes();
		if (registeredTypes.contains(firstType))
			return true;
		return false;
	}

	MountManager getMountManager() {
		return mountManager;
	}

	TypesManager getTypesManager() {
		return typesManager;
	}
}
