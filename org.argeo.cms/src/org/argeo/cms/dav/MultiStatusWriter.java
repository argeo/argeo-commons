package org.argeo.cms.dav;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class MultiStatusWriter implements Consumer<DavResponse> {
	private BlockingQueue<DavResponse> queue = new ArrayBlockingQueue<>(64);

//	private OutputStream out;

	private Thread processingThread;

	private AtomicBoolean done = new AtomicBoolean(false);

	private AtomicBoolean polling = new AtomicBoolean();

	public void process(NamespaceContext namespaceContext, OutputStream out, CompletionStage<Void> published,
			boolean propname) throws IOException {
		published.thenRun(() -> allPublished());
		processingThread = Thread.currentThread();
//		this.out = out;

		try {
			XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
			XMLStreamWriter xsWriter = xmlOutputFactory.createXMLStreamWriter(out, StandardCharsets.UTF_8.name());
			xsWriter.setNamespaceContext(namespaceContext);
			xsWriter.setDefaultNamespace(DavXmlElement.WEBDAV_NAMESPACE_URI);

			xsWriter.writeStartDocument();
			DavXmlElement.multistatus.startElement(xsWriter);
			xsWriter.writeDefaultNamespace(DavXmlElement.WEBDAV_NAMESPACE_URI);

			poll: while (!(done.get() && queue.isEmpty())) {
				DavResponse davResponse;
				try {
					polling.set(true);
					davResponse = queue.poll(10, TimeUnit.MILLISECONDS);
					if (davResponse == null)
						continue poll;
					System.err.println(davResponse.getHref());
				} catch (InterruptedException e) {
					System.err.println(e);
					continue poll;
				} finally {
					polling.set(false);
				}

				writeDavResponse(xsWriter, davResponse, propname);
			}

			xsWriter.writeEndElement();// multistatus
			xsWriter.writeEndDocument();
			xsWriter.close();
			out.close();
		} catch (FactoryConfigurationError | XMLStreamException e) {
			synchronized (this) {
				processingThread = null;
			}
		}
	}

	protected void writeDavResponse(XMLStreamWriter xsWriter, DavResponse davResponse, boolean propname)
			throws XMLStreamException {
		Set<String> namespaces = new HashSet<>();
		for (QName key : davResponse.getPropertyNames()) {
			if (key.getNamespaceURI().equals(DavXmlElement.WEBDAV_NAMESPACE_URI))
				continue; // skip
			if (key.getNamespaceURI().equals(XMLConstants.W3C_XML_SCHEMA_NS_URI))
				continue; // skip
			namespaces.add(key.getNamespaceURI());
		}
		DavXmlElement.response.startElement(xsWriter);
		// namespaces
		for (String ns : namespaces)
			xsWriter.writeNamespace(xsWriter.getNamespaceContext().getPrefix(ns), ns);

		DavXmlElement.href.setSimpleValue(xsWriter, davResponse.getHref());

		{
			DavXmlElement.propstat.startElement(xsWriter);
			{
				DavXmlElement.prop.startElement(xsWriter);
				if (!davResponse.getResourceTypes().isEmpty() || davResponse.isCollection()) {
					DavXmlElement.resourcetype.startElement(xsWriter);
					if (davResponse.isCollection())
						DavXmlElement.collection.emptyElement(xsWriter);
					for (QName resourceType : davResponse.getResourceTypes()) {
						xsWriter.writeEmptyElement(resourceType.getNamespaceURI(), resourceType.getLocalPart());
					}
					xsWriter.writeEndElement();// resource type
				}
				for (QName key : davResponse.getPropertyNames()) {
					if (propname) {
						xsWriter.writeEmptyElement(key.getNamespaceURI(), key.getLocalPart());
					} else {
						xsWriter.writeStartElement(key.getNamespaceURI(), key.getLocalPart());
						xsWriter.writeCData(davResponse.getProperties().get(key));
						xsWriter.writeEndElement();
					}
				}
				xsWriter.writeEndElement();// prop
			}
			xsWriter.writeEndElement();// propstat
		}
		xsWriter.writeEndElement();// response
	}

	@Override
	public void accept(DavResponse davResponse) {
		try {
			queue.put(davResponse);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected synchronized void allPublished() {
		done.set(true);
		if (processingThread != null && queue.isEmpty() && polling.get()) {
			// we only interrupt if the queue is already processed
			// so as not to interrupt I/O
			processingThread.interrupt();
		}
	}

}
