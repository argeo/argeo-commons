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
package org.argeo.jcr.ui.explorer.browser;

import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.utils.JcrItemsComparator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class PropertiesContentProvider implements IStructuredContentProvider {
	private JcrItemsComparator itemComparator = new JcrItemsComparator();

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public Object[] getElements(Object inputElement) {
		try {
			if (inputElement instanceof Node) {
				Set<Property> props = new TreeSet<Property>(itemComparator);
				PropertyIterator pit = ((Node) inputElement).getProperties();
				while (pit.hasNext())
					props.add(pit.nextProperty());
				return props.toArray();
			}
			return new Object[] {};
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get element for " + inputElement,
					e);
		}
	}

}
