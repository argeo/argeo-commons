package org.argeo.cms.jcr.acr;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.CmsLog;
import org.argeo.jcr.Jcr;
import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Utilities around integration between JCR and ACR. */
public class JcrContentUtils {
	private final static CmsLog log = CmsLog.getLog(JcrContentUtils.class);

	public static void copyFiles(Node folder, Content collection, String... additionalCollectionTypes) {
		try {
			log.debug("Copy collection " + collection);

			NamespaceContext jcrNamespaceContext = new JcrSessionNamespaceContext(folder.getSession());

			nodes: for (NodeIterator it = folder.getNodes(); it.hasNext();) {
				Node node = it.nextNode();
				String name = node.getName();
				if (node.isNodeType(NodeType.NT_FILE)) {
					Content file = collection.anyOrAddChild(new ContentName(name));
					try (InputStream in = JcrUtils.getFileAsStream(node)) {
						file.write(InputStream.class).complete(in);
					}
				} else if (node.isNodeType(NodeType.NT_FOLDER)) {
					Content subCol = collection.add(name, CrName.collection.qName());
					copyFiles(node, subCol, additionalCollectionTypes);
				} else {
					List<QName> contentClasses = typesAsContentClasses(node, jcrNamespaceContext);
					for (String collectionType : additionalCollectionTypes) {
						if (node.isNodeType(collectionType)) {
							contentClasses.add(CrName.collection.qName());
							Content subCol = collection.add(name,
									contentClasses.toArray(new QName[contentClasses.size()]));
							setAttributes(node, subCol, jcrNamespaceContext);
//							setContentClasses(node, subCol, jcrNamespaceContext);
							copyFiles(node, subCol, additionalCollectionTypes);
							continue nodes;
						}
					}

					QName qName = NamespaceUtils.parsePrefixedName(name);
//					if (NamespaceUtils.hasNamespace(qName)) {
					if (node.getIndex() > 1) {
						log.warn("Same name siblings not supported, skipping " + node);
						continue nodes;
					}
					Content content = collection.add(qName, contentClasses.toArray(new QName[contentClasses.size()]));
					Source source = toSource(node);
					((ProvidedContent) content).getSession().edit((s) -> {
						((ProvidedSession) s).notifyModification((ProvidedContent) content);
						content.write(Source.class).complete(source);
//						try {
//							//setContentClasses(node, content, jcrNamespaceContext);
//						} catch (RepositoryException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
					}).toCompletableFuture().join();
					setAttributes(node, content, jcrNamespaceContext);

//					} else {
//						// ignore
//						log.debug(() -> "Ignored " + node);
//						continue nodes;
//					}
				}
			}
		} catch (RepositoryException e) {
			throw new JcrException("Cannot copy files from " + folder + " to " + collection, e);
		} catch (IOException e) {
			throw new RuntimeException("Cannot copy files from " + folder + " to " + collection, e);
		}
	}

	private static Source toSource(Node node) throws RepositoryException {

//		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//			node.getSession().exportDocumentView(node.getPath(), out, true, false);
//			System.out.println(new String(out.toByteArray(), StandardCharsets.UTF_8));
////			DocumentBuilder documentBuilder = DocumentBuilderFactory.newNSInstance().newDocumentBuilder();
////			Document document;
////			try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
////				document = documentBuilder.parse(in);
////			}
////			cleanJcrDom(document);
////			return new DOMSource(document);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

		try (PipedInputStream in = new PipedInputStream();) {

			CompletableFuture<Document> toDo = CompletableFuture.supplyAsync(() -> {
				try {
					DocumentBuilder documentBuilder = DocumentBuilderFactory.newNSInstance().newDocumentBuilder();
					return documentBuilder.parse(in);
				} catch (ParserConfigurationException | SAXException | IOException e) {
					throw new RuntimeException("Cannot parse", e);
				}
			});

			// TODO optimise
			try (PipedOutputStream out = new PipedOutputStream(in)) {
				node.getSession().exportDocumentView(node.getPath(), out, true, false);
			} catch (IOException | RepositoryException e) {
				throw new RuntimeException("Cannot export " + node + " in workspace " + Jcr.getWorkspaceName(node), e);
			}
			Document document = toDo.get();
			cleanJcrDom(document);
			return new DOMSource(document);
		} catch (IOException | InterruptedException | ExecutionException e1) {
			throw new RuntimeException("Cannot parse", e1);
		}

	}

	public static void setAttributes(Node source, Content target, NamespaceContext jcrNamespaceContext)
			throws RepositoryException {
		properties: for (PropertyIterator pit = source.getProperties(); pit.hasNext();) {
			Property p = pit.nextProperty();
			// TODO migrate JCR title, last modified, etc. ?
			if (p.getName().startsWith("jcr:"))
				continue properties;
			if (p.isMultiple()) {
				List<String> attr = new ArrayList<>();
				for (Value value : p.getValues()) {
					attr.add(value.getString());
				}
				target.put(NamespaceUtils.parsePrefixedName(jcrNamespaceContext, p.getName()), attr);
			} else {
				target.put(NamespaceUtils.parsePrefixedName(jcrNamespaceContext, p.getName()), p.getString());
			}
		}
	}

	public static List<QName> typesAsContentClasses(Node source, NamespaceContext jcrNamespaceContext)
			throws RepositoryException {
		// TODO super types?
		List<QName> contentClasses = new ArrayList<>();
		contentClasses
				.add(NamespaceUtils.parsePrefixedName(jcrNamespaceContext, source.getPrimaryNodeType().getName()));
		for (NodeType nodeType : source.getMixinNodeTypes()) {
			contentClasses.add(NamespaceUtils.parsePrefixedName(jcrNamespaceContext, nodeType.getName()));
		}
		// filter out JCR types
		for (Iterator<QName> it = contentClasses.iterator(); it.hasNext();) {
			QName type = it.next();
			if (type.getNamespaceURI().equals(JCR_NT_NAMESPACE_URI)
					|| type.getNamespaceURI().equals(JCR_MIX_NAMESPACE_URI)) {
				it.remove();
			}
		}
		// target.addContentClasses(contentClasses.toArray(new
		// QName[contentClasses.size()]));
		return contentClasses;
	}

	static final String JCR_NAMESPACE_URI = "http://www.jcp.org/jcr/1.0";
	static final String JCR_NT_NAMESPACE_URI = "http://www.jcp.org/jcr/nt/1.0";
	static final String JCR_MIX_NAMESPACE_URI = "http://www.jcp.org/jcr/mix/1.0";

	public static void cleanJcrDom(Document document) {
		Element documentElement = document.getDocumentElement();
		Set<String> namespaceUris = new HashSet<>();
		cleanJcrDom(documentElement, namespaceUris);

		// remove unused namespaces
		NamedNodeMap attrs = documentElement.getAttributes();
		Set<Attr> toRemove = new HashSet<>();
		for (int i = 0; i < attrs.getLength(); i++) {
			Attr attr = (Attr) attrs.item(i);
//			log.debug("Check "+i+" " + attr);
			String prefix = attr.getPrefix();
			if (prefix != null && prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
				String namespaceUri = attr.getValue();
				if (!namespaceUris.contains(namespaceUri)) {
					toRemove.add(attr);
					// log.debug("Removing "+i+" " + namespaceUri);
				}
			}
		}
		for (Attr attr : toRemove)
			documentElement.removeAttributeNode(attr);

	}

	private static void cleanJcrDom(Element element, Set<String> namespaceUris) {
		NodeList children = element.getElementsByTagName("*");
		for (int i = 0; i < children.getLength(); i++) {
			Element child = (Element) children.item(i);
			if (!namespaceUris.contains(child.getNamespaceURI()))
				namespaceUris.add(child.getNamespaceURI());
			cleanJcrDom(child, namespaceUris);
		}

		NamedNodeMap attrs = element.getAttributes();
		attributes: for (int i = 0; i < attrs.getLength(); i++) {
			Attr attr = (Attr) attrs.item(i);
			String namespaceUri = attr.getNamespaceURI();
			if (namespaceUri == null)
				continue attributes;
			if (JCR_NAMESPACE_URI.equals(namespaceUri)) {
				// FIXME probably wrong to change attributes length
				element.removeAttributeNode(attr);
				continue attributes;
			}
			if (!namespaceUris.contains(namespaceUri))
				namespaceUris.add(attr.getNamespaceURI());

		}

	}

	/** singleton */
	private JcrContentUtils() {
	}

}
