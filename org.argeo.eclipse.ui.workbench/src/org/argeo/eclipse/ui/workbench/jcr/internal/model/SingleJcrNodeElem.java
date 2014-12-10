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
package org.argeo.eclipse.ui.workbench.jcr.internal.model;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;

/**
 * UI Tree component. Wraps a node of a JCR {@link Workspace}. It also keeps a
 * reference to its parent node that can either be a {@link WorkspaceElem}, a
 * {@link SingleJcrNodeElem} or null if the node is "mounted" as the root of the UI
 * tree.
 */

public class SingleJcrNodeElem extends TreeParent {

	private final Node node;
	private String alias = null;

	// keeps a local reference to the node's name to avoid exception when the
	// session is lost
	// private final String name;

	/** Creates a new UiNode in the UI Tree */
	public SingleJcrNodeElem(TreeParent parent, Node node, String name) {
		super(name);
		setParent(parent);
		this.node = node;
	}

	/**
	 * Creates a new UiNode in the UI Tree, keeping a reference to the alias of
	 * the corresponding repository in the current UI environment. It is useful
	 * to be able to mount nodes as roots of the UI tree.
	 */
	public SingleJcrNodeElem(TreeParent parent, Node node, String name, String alias) {
		super(name);
		setParent(parent);
		this.node = node;
		this.alias = alias;
	}

	/** returns the node wrapped by the current Ui object */
	public Node getNode() {
		return node;
	}

	protected String getRepositoryAlias() {
		return alias;
	}

	/**
	 * Override normal behavior to initialize children only when first requested
	 */
	@Override
	public synchronized Object[] getChildren() {
		if (isLoaded()) {
			return super.getChildren();
		} else {
			// initialize current object
			try {
				NodeIterator ni = node.getNodes();
				while (ni.hasNext()) {
					Node curNode = ni.nextNode();
					addChild(new SingleJcrNodeElem(this, curNode, curNode.getName()));
				}
				return super.getChildren();
			} catch (RepositoryException re) {
				throw new ArgeoException(
						"Unexcpected error while initializing children SingleJcrNode",
						re);
			}
		}
	}

	@Override
	public boolean hasChildren() {
		try {
			if (node.getSession().isLive())
				return node.hasNodes();
			else
				return false;
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while checking children node existence",
					re);
		}
	}

}
