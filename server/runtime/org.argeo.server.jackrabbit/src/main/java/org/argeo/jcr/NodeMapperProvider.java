package org.argeo.jcr;

import javax.jcr.Node;

/** Provides a node mapper relevant for this node. */
public interface NodeMapperProvider {
	/** @return the node mapper or null if no relvant node mapper cna be found. */
	public NodeMapper findNodeMapper(Node node);
}
