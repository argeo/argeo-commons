package org.argeo.cms.dav;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.argeo.cms.http.HttpStatus;

/**
 * Asynchronously iterate over the response statuses of the response to a
 * PROPFIND request.
 */
class MultiStatusReader implements Iterator<DavResponse> {
	private CompletableFuture<Boolean> empty = new CompletableFuture<Boolean>();
	private AtomicBoolean processed = new AtomicBoolean(false);

	private BlockingQueue<DavResponse> queue = new ArrayBlockingQueue<>(64);

	private final String ignoredHref;

	public MultiStatusReader(InputStream in) {
		this(in, null);
	}

	/** Typically ignoring self */
	public MultiStatusReader(InputStream in, String ignoredHref) {
		this.ignoredHref = ignoredHref;
		ForkJoinPool.commonPool().execute(() -> process(in));
	}

	protected void process(InputStream in) {
		try {
			XMLInputFactory inputFactory = XMLInputFactory.newFactory();
			XMLStreamReader reader = inputFactory.createXMLStreamReader(in, StandardCharsets.UTF_8.name());

			DavResponse currentResponse = null;
			boolean collectiongProperties = false;
			Set<QName> currentPropertyNames = null;
			HttpStatus currentStatus = null;

			final QName COLLECTION = DavXmlElement.collection.qName(); // optimisation
			elements: while (reader.hasNext()) {
				reader.next();
				if (reader.isStartElement()) {
					QName name = reader.getName();
//				System.out.println(name);
					DavXmlElement davXmlElement = DavXmlElement.toEnum(name);
					if (davXmlElement != null) {
						switch (davXmlElement) {
						case response:
							currentResponse = new DavResponse();
							break;
						case href:
							assert currentResponse != null;
							while (reader.hasNext() && !reader.hasText())
								reader.next();
							String href = reader.getText();
							currentResponse.setHref(href);
							break;
//						case collection:
//							currentResponse.setCollection(true);
//							break;
						case status:
							reader.next();
							String statusLine = reader.getText();
							currentStatus = HttpStatus.parseStatusLine(statusLine);
							break;
						case prop:
							collectiongProperties = true;
							currentPropertyNames = new HashSet<>();
							break;
						case resourcetype:
							while (reader.hasNext()) {
								int event = reader.nextTag();
								QName resourceType = reader.getName();
								if (event == XMLStreamConstants.END_ELEMENT && name.equals(resourceType))
									break;
								assert currentResponse != null;
								if (event == XMLStreamConstants.START_ELEMENT) {
									if (COLLECTION.equals(resourceType))
										currentResponse.setCollection(true);
									else
										currentResponse.getResourceTypes().add(resourceType);
								}
							}
							break;
						default:
							// ignore
						}
					} else {
						if (collectiongProperties) {
							String value = null;
							// TODO deal with complex properties
							readProperty: while (reader.hasNext()) {
								reader.next();
								if (reader.getEventType() == XMLStreamConstants.END_ELEMENT)
									break readProperty;
								if (reader.getEventType() == XMLStreamConstants.CHARACTERS)
									value = reader.getText();
							}

							if (name.getNamespaceURI().equals(DavResponse.MOD_DAV_NAMESPACE))
								continue elements; // skip mod_dav properties

							assert currentResponse != null;
							currentPropertyNames.add(name);
							if (value != null)
								currentResponse.getProperties().put(name, value);

						}
					}
				} else if (reader.isEndElement()) {
					QName name = reader.getName();
//					System.out.println(name);
					DavXmlElement davXmlElement = DavXmlElement.toEnum(name);
					if (davXmlElement != null)
						switch (davXmlElement) {
						case propstat:
							currentResponse.getPropertyNames(currentStatus).addAll(currentPropertyNames);
							currentPropertyNames = null;
							break;
						case response:
							assert currentResponse != null;
							if (ignoredHref == null || !ignoredHref.equals(currentResponse.getHref())) {
								if (!empty.isDone())
									empty.complete(false);
								publish(currentResponse);
							}
						case prop:
							collectiongProperties = false;
							break;
						default:
							// ignore
						}
				}
			}

			if (!empty.isDone())
				empty.complete(true);
		} catch (FactoryConfigurationError | XMLStreamException e) {
			empty.completeExceptionally(e);
			throw new IllegalStateException("Cannot process DAV response", e);
		} finally {
			processed();
		}
	}

	protected synchronized void publish(DavResponse response) {
		try {
			queue.put(response);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Cannot put response " + response, e);
		} finally {
			notifyAll();
		}
	}

	protected synchronized void processed() {
		processed.set(true);
		notifyAll();
	}

	@Override
	public synchronized boolean hasNext() {
		try {
			if (empty.get())
				return false;
			while (!processed.get() && queue.isEmpty()) {
				wait();
			}
			if (!queue.isEmpty())
				return true;
			if (processed.get())
				return false;
			throw new IllegalStateException("Cannot determine hasNext");
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException("Cannot determine hasNext", e);
		} finally {
			// notifyAll();
		}
	}

	@Override
	public synchronized DavResponse next() {
		try {
			if (!hasNext())
				throw new IllegalStateException("No fursther items are available");

			DavResponse response = queue.take();
			return response;
		} catch (InterruptedException e) {
			throw new IllegalStateException("Cannot get next", e);
		} finally {
			// notifyAll();
		}
	}

}
