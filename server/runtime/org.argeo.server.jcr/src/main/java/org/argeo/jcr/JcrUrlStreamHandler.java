/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

/** URL stream handler able to deal with nt:file node and properties. NOT FINISHED */
public class JcrUrlStreamHandler extends URLStreamHandler {
	private final Session session;

	public JcrUrlStreamHandler(Session session) {
		this.session = session;
	}

	@Override
	protected URLConnection openConnection(final URL u) throws IOException {
		// TODO Auto-generated method stub
		return new URLConnection(u) {

			@Override
			public void connect() throws IOException {
				String itemPath = u.getPath();
				try {
					if (!session.itemExists(itemPath))
						throw new IOException("No item under " + itemPath);

					Item item = session.getItem(u.getPath());
					if (item.isNode()) {
						// this should be a nt:file node
						Node node = (Node) item;
						if (!node.getPrimaryNodeType().isNodeType(
								NodeType.NT_FILE))
							throw new IOException("Node " + node + " is not a "
									+ NodeType.NT_FILE);

					} else {
						Property property = (Property) item;
						if(property.getType()==PropertyType.BINARY){
							//Binary binary = property.getBinary();
							
						}
					}
				} catch (RepositoryException e) {
					IOException ioe = new IOException(
							"Unexpected JCR exception");
					ioe.initCause(e);
					throw ioe;
				}
			}

			@Override
			public InputStream getInputStream() throws IOException {
				// TODO Auto-generated method stub
				return super.getInputStream();
			}

		};
	}

}
