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
package org.argeo.eclipse.ui;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Tree content provider dealing with tree objects and providing reasonable
 * defaults.
 */
public abstract class AbstractTreeContentProvider implements
		ITreeContentProvider {
	private static final long serialVersionUID = 8246126401957763868L;

	/** Does nothing */
	public void dispose() {
	}

	/** Does nothing */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public Object[] getChildren(Object element) {
		if (element instanceof TreeParent) {
			return ((TreeParent) element).getChildren();
		}
		return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof TreeParent) {
			return ((TreeParent) element).getParent();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof TreeParent) {
			return ((TreeParent) element).hasChildren();
		}
		return false;
	}

}
