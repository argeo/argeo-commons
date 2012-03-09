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

import javax.jcr.Node;

/** Provides a node mapper relevant for this node. */
public interface NodeMapperProvider {

	/** 
	 * Node Mapper is chosen regarding the Jcr path of the node parameter 
	 * @param Node node
	 * @return the node mapper or null if no relevant node mapper can be found. */
	public NodeMapper findNodeMapper(Node node);
}
