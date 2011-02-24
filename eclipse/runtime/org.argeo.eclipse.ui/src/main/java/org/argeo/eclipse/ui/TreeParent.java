/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
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

import java.util.ArrayList;
import java.util.List;

public class TreeParent extends TreeObject {
	private List<TreeObject> children;

	private boolean loaded;

	public TreeParent(String name) {
		super(name);
		children = new ArrayList<TreeObject>();
		loaded = false;
	}

	public synchronized void addChild(TreeObject child) {
		loaded = true;
		children.add(child);
		child.setParent(this);
	}

	public synchronized void removeChild(TreeObject child) {
		children.remove(child);
		child.setParent(null);
	}

	public synchronized void clearChildren() {
		loaded = false;
		children.clear();
	}

	public synchronized TreeObject[] getChildren() {
		return (TreeObject[]) children.toArray(new TreeObject[children.size()]);
	}

	public synchronized boolean hasChildren() {
		return children.size() > 0;
	}

	public TreeObject getChildByName(String name) {
		for (TreeObject child : children) {
			if (child.getName().equals(name))
				return child;
		}
		return null;
	}

	public synchronized Boolean isLoaded() {
		return loaded;
	}
}
