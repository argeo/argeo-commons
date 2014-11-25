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
package org.argeo.eclipse.ui.jcr.utils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.eclipse.jface.viewers.IElementComparer;

/** Compare JCR nodes based on their JCR identifiers, for use in JFace viewers. */
public class NodeViewerComparer implements IElementComparer {

	// force comparison on Node IDs only.
	public boolean equals(Object elementA, Object elementB) {
		if (!(elementA instanceof Node) || !(elementB instanceof Node)) {
			return elementA == null ? elementB == null : elementA
					.equals(elementB);
		} else {

			boolean result = false;
			try {
				String idA = ((Node) elementA).getIdentifier();
				String idB = ((Node) elementB).getIdentifier();
				result = idA == null ? idB == null : idA.equals(idB);
			} catch (RepositoryException re) {
				throw new ArgeoException("cannot compare nodes", re);
			}

			return result;
		}
	}

	public int hashCode(Object element) {
		// TODO enhanced this method.
		return element.getClass().toString().hashCode();
	}
}