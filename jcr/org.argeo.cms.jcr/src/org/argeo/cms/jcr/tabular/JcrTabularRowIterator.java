package org.argeo.cms.jcr.tabular;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.argeo.cms.ArgeoTypes;
import org.argeo.cms.tabular.ArrayTabularRow;
import org.argeo.cms.tabular.TabularColumn;
import org.argeo.cms.tabular.TabularRow;
import org.argeo.cms.tabular.TabularRowIterator;
import org.argeo.jcr.JcrException;
import org.argeo.util.CsvParser;

/** Iterates over the rows of a {@link ArgeoTypes#ARGEO_TABLE} node. */
public class JcrTabularRowIterator implements TabularRowIterator {
	private Boolean hasNext = null;
	private Boolean parsingCompleted = false;

	private Long currentRowNumber = 0l;

	private List<TabularColumn> header = new ArrayList<TabularColumn>();

	/** referenced so that we can close it */
	private Binary binary;
	private InputStream in;

	private CsvParser csvParser;
	private ArrayBlockingQueue<List<String>> textLines;

	public JcrTabularRowIterator(Node tableNode) {
		try {
			for (NodeIterator it = tableNode.getNodes(); it.hasNext();) {
				Node node = it.nextNode();
				if (node.isNodeType(ArgeoTypes.ARGEO_COLUMN)) {
					Integer type = PropertyType.valueFromName(node.getProperty(
							Property.JCR_REQUIRED_TYPE).getString());
					TabularColumn tc = new TabularColumn(node.getProperty(
							Property.JCR_TITLE).getString(), type);
					header.add(tc);
				}
			}
			Node contentNode = tableNode.getNode(Property.JCR_CONTENT);
			if (contentNode.isNodeType(ArgeoTypes.ARGEO_CSV)) {
				textLines = new ArrayBlockingQueue<List<String>>(1000);
				csvParser = new CsvParser() {
					protected void processLine(Integer lineNumber,
							List<String> header, List<String> tokens) {
						try {
							textLines.put(tokens);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// textLines.add(tokens);
						if (hasNext == null) {
							hasNext = true;
							synchronized (JcrTabularRowIterator.this) {
								JcrTabularRowIterator.this.notifyAll();
							}
						}
					}
				};
				csvParser.setNoHeader(true);
				binary = contentNode.getProperty(Property.JCR_DATA).getBinary();
				in = binary.getStream();
				Thread thread = new Thread(contentNode.getPath() + " reader") {
					public void run() {
						try {
							csvParser.parse(in);
						} finally {
							parsingCompleted = true;
							IOUtils.closeQuietly(in);
						}
					}
				};
				thread.start();
			}
		} catch (RepositoryException e) {
			throw new JcrException("Cannot read table " + tableNode, e);
		}
	}

	public synchronized boolean hasNext() {
		// we don't know if there is anything available
		// while (hasNext == null)
		// try {
		// wait();
		// } catch (InterruptedException e) {
		// // silent
		// // FIXME better deal with interruption
		// Thread.currentThread().interrupt();
		// break;
		// }

		// buffer not empty
		if (!textLines.isEmpty())
			return true;

		// maybe the parsing is finished but the flag has not been set
		while (!parsingCompleted && textLines.isEmpty())
			try {
				wait(100);
			} catch (InterruptedException e) {
				// silent
				// FIXME better deal with interruption
				Thread.currentThread().interrupt();
				break;
			}

		// buffer not empty
		if (!textLines.isEmpty())
			return true;

		// (parsingCompleted && textLines.isEmpty())
		return false;

		// if (!hasNext && textLines.isEmpty()) {
		// if (in != null) {
		// IOUtils.closeQuietly(in);
		// in = null;
		// }
		// if (binary != null) {
		// JcrUtils.closeQuietly(binary);
		// binary = null;
		// }
		// return false;
		// } else
		// return true;
	}

	public synchronized TabularRow next() {
		try {
			List<String> tokens = textLines.take();
			List<Object> objs = new ArrayList<Object>(tokens.size());
			for (String token : tokens) {
				// TODO convert to other formats using header
				objs.add(token);
			}
			currentRowNumber++;
			return new ArrayTabularRow(objs);
		} catch (InterruptedException e) {
			// silent
			// FIXME better deal with interruption
		}
		return null;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public Long getCurrentRowNumber() {
		return currentRowNumber;
	}

	public List<TabularColumn> getHeader() {
		return header;
	}

}
