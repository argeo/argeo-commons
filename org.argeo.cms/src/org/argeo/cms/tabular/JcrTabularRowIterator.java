/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.cms.tabular;

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
import org.argeo.jcr.ArgeoJcrException;
import org.argeo.node.ArgeoTypes;
import org.argeo.node.tabular.ArrayTabularRow;
import org.argeo.node.tabular.TabularColumn;
import org.argeo.node.tabular.TabularRow;
import org.argeo.node.tabular.TabularRowIterator;
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
			throw new ArgeoJcrException("Cannot read table " + tableNode, e);
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
