package org.argeo.jcr;

import javax.jcr.Node;

/** Provides a node mapper relevant for this node. */
public interface NodeMapperProvider {

	/** 
	 * Node Mapper is chosen regarding the Jcr path of the node parameter 
	 * @param Node node
	 * @return the node mapper or null if no relevant node mapper can be found. */
	public NodeMapper findNodeMapper(Node node);
}
