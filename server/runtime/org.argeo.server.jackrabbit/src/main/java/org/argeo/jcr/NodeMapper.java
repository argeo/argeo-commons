package org.argeo.jcr;

import javax.jcr.Node;
import javax.jcr.Session;

public interface NodeMapper {
	public Object load(Node node);

	public void update(Node node, Object obj);

	public Node save(Session session, String path, Object obj);
	
	public void setNodeMapperProvider(NodeMapperProvider nmp);
}
