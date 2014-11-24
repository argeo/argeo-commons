package org.argeo.cms.viewers;

import javax.jcr.Node;

/** An editable part related to a node */
public interface NodePart extends ItemPart<Node> {
	public Node getNode();
}
