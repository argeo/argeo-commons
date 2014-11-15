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
package org.argeo.jcr;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

/** Wraps a collection of nodes in order to read it as a {@link NodeIterator} */
public class CollectionNodeIterator implements NodeIterator {
	private final Long collectionSize;
	private final Iterator<Node> iterator;
	private Integer position = 0;

	public CollectionNodeIterator(Collection<Node> nodes) {
		super();
		this.collectionSize = (long) nodes.size();
		this.iterator = nodes.iterator();
	}

	public void skip(long skipNum) {
		if (skipNum < 0)
			throw new IllegalArgumentException(
					"Skip count has to be positive: " + skipNum);

		for (long i = 0; i < skipNum; i++) {
			if (!hasNext())
				throw new NoSuchElementException("Last element past (position="
						+ getPosition() + ")");
			nextNode();
		}
	}

	public long getSize() {
		return collectionSize;
	}

	public long getPosition() {
		return position;
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}

	public Object next() {
		return nextNode();
	}

	public void remove() {
		iterator.remove();
	}

	public Node nextNode() {
		Node node = iterator.next();
		position++;
		return node;
	}

}
