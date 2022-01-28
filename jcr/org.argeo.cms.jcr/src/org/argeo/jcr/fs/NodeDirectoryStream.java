package org.argeo.jcr.fs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

public class NodeDirectoryStream implements DirectoryStream<Path> {
	private final JcrFileSystem fs;
	private final NodeIterator nodeIterator;
	private final Iterator<JcrPath> additionalPaths;
	private final Filter<? super Path> filter;

	public NodeDirectoryStream(JcrFileSystem fs, NodeIterator nodeIterator, Iterator<JcrPath> additionalPaths,
			Filter<? super Path> filter) {
		this.fs = fs;
		this.nodeIterator = nodeIterator;
		this.additionalPaths = additionalPaths;
		this.filter = filter;
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public Iterator<Path> iterator() {
		return new Iterator<Path>() {
			private JcrPath next = null;

			@Override
			public synchronized boolean hasNext() {
				if (next != null)
					return true;
				nodes: while (nodeIterator.hasNext()) {
					try {
						Node node = nodeIterator.nextNode();
						String nodeName = node.getName();
						if (nodeName.startsWith("rep:") || nodeName.startsWith("jcr:"))
							continue nodes;
						if (fs.skipNode(node))
							continue nodes;
						next = new JcrPath(fs, node);
						if (filter != null) {
							if (filter.accept(next))
								break nodes;
						} else
							break nodes;
					} catch (Exception e) {
						throw new JcrFsException("Could not get next path", e);
					}
				}

				if (next == null) {
					if (additionalPaths.hasNext())
						next = additionalPaths.next();
				}

				return next != null;
			}

			@Override
			public synchronized Path next() {
				if (!hasNext())// make sure has next has been called
					return null;
				JcrPath res = next;
				next = null;
				return res;
			}

		};
	}

}
