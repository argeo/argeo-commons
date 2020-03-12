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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.argeo.api.tabular.TabularColumn;
import org.argeo.api.tabular.TabularWriter;
import org.argeo.cms.ArgeoTypes;
import org.argeo.jcr.ArgeoJcrException;
import org.argeo.jcr.JcrUtils;
import org.argeo.util.CsvWriter;

/** Write / reference tabular content in a JCR repository. */
public class JcrTabularWriter implements TabularWriter {
	private Node contentNode;
	private ByteArrayOutputStream out;
	private CsvWriter csvWriter;
	
	@SuppressWarnings("unused")
	private final List<TabularColumn> columns;

	/** Creates a table node */
	public JcrTabularWriter(Node tableNode, List<TabularColumn> columns,
			String contentNodeType) {
		try {
			this.columns = columns;
			for (TabularColumn column : columns) {
				String normalized = JcrUtils.replaceInvalidChars(column
						.getName());
				Node columnNode = tableNode.addNode(normalized,
						ArgeoTypes.ARGEO_COLUMN);
				columnNode.setProperty(Property.JCR_TITLE, column.getName());
				if (column.getType() != null)
					columnNode.setProperty(Property.JCR_REQUIRED_TYPE,
							PropertyType.nameFromValue(column.getType()));
				else
					columnNode.setProperty(Property.JCR_REQUIRED_TYPE,
							PropertyType.TYPENAME_STRING);
			}
			contentNode = tableNode.addNode(Property.JCR_CONTENT,
					contentNodeType);
			if (contentNodeType.equals(ArgeoTypes.ARGEO_CSV)) {
				contentNode.setProperty(Property.JCR_MIMETYPE, "text/csv");
				contentNode.setProperty(Property.JCR_ENCODING, "UTF-8");
				out = new ByteArrayOutputStream();
				csvWriter = new CsvWriter(out);
			}
		} catch (RepositoryException e) {
			throw new ArgeoJcrException("Cannot create table node " + tableNode, e);
		}
	}

	public void appendRow(Object[] row) {
		csvWriter.writeLine(row);
	}

	public void close() {
		Binary binary = null;
		InputStream in = null;
		try {
			// TODO parallelize with pipes and writing from another thread
			in = new ByteArrayInputStream(out.toByteArray());
			binary = contentNode.getSession().getValueFactory()
					.createBinary(in);
			contentNode.setProperty(Property.JCR_DATA, binary);
		} catch (RepositoryException e) {
			throw new ArgeoJcrException("Cannot store data in " + contentNode, e);
		} finally {
			IOUtils.closeQuietly(in);
			JcrUtils.closeQuietly(binary);
		}
	}
}
