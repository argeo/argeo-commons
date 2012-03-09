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
package org.argeo.demo.i18n.providers;

import java.util.ArrayList;
import java.util.List;

import org.argeo.demo.i18n.model.Place;
import org.argeo.eclipse.ui.TreeParent;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Implementation of the {@code ITreeContentProvider} to display multiple
 * repository environment in a tree like structure
 * 
 */
public class SimpleContentProvider implements ITreeContentProvider {
	// private final static Log log =
	// LogFactory.getLog(SimpleContentProvider.class);

	public SimpleContentProvider() {
	}

	/**
	 * Sends back the first level of the Tree. Independent from inputElement
	 * that can be null. Values are hard coded here.
	 */
	public Object[] getElements(Object inputElement) {
		List<Object> objs = new ArrayList<Object>();
		objs.add(new Place("Home", "My house, my family",
				"12 rue du bac, Paris"));
		objs.add(new Place("Office", "Where I work",
				"100 av des champs Elysées"));
		objs.add(new Place("School",
				"The place where the children spend their days",
				"103 Avenue montaigne, Paris"));
		return objs.toArray();
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TreeParent)
			return ((TreeParent) parentElement).getChildren();
		else {
			return new Object[0];
		}
	}

	public Object getParent(Object element) {
		if (element instanceof TreeParent) {
			return ((TreeParent) element).getParent();
		} else
			return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof TreeParent) {
			TreeParent tp = (TreeParent) element;
			return tp.hasChildren();
		}
		return false;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
