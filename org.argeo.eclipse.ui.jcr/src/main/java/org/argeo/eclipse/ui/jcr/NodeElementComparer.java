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
package org.argeo.eclipse.ui.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.eclipse.jface.viewers.IElementComparer;

/** Element comparer for JCR node, to be used in JFace viewers. */
public class NodeElementComparer implements IElementComparer {

	public boolean equals(Object a, Object b) {
		try {
			if ((a instanceof Node) && (b instanceof Node)) {
				Node nodeA = (Node) a;
				Node nodeB = (Node) b;
				return nodeA.getIdentifier().equals(nodeB.getIdentifier());
			} else {
				return a.equals(b);
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot compare nodes", e);
		}
	}

	public int hashCode(Object element) {
		try {
			if (element instanceof Node)
				return ((Node) element).getIdentifier().hashCode();
			return element.hashCode();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get hash code", e);
		}
	}

}
