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
