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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;

import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoJcrUtils;
import org.argeo.jcr.RepositoryRegister;
import org.argeo.jcr.security.JcrKeyring;
import org.argeo.jcr.ui.explorer.model.RepositoriesNode;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.argeo.jcr.ui.explorer.utils.TreeObjectsComparator;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Implementation of the {@code ITreeContentProvider} to display multiple
 * repository environment in a tree like structure
 * 
 */
public class NodeContentProvider implements ITreeContentProvider {
	// private final static Log log =
	// LogFactory.getLog(NodeContentProvider.class);

	// Business Objects
	final private RepositoryRegister repositoryRegister;
	final private RepositoryFactory repositoryFactory;
	final private Session userSession;
	final private JcrKeyring jcrKeyring;
	final private boolean sortChildren;

	// reference for cleaning
	private SingleJcrNode homeNode = null;
	private RepositoriesNode repositoriesNode = null;

	// Utils
	private TreeObjectsComparator itemComparator = new TreeObjectsComparator();

	public NodeContentProvider(JcrKeyring jcrKeyring,
			RepositoryRegister repositoryRegister,
			RepositoryFactory repositoryFactory, Boolean sortChildren) {
		this.userSession = jcrKeyring != null ? jcrKeyring.getSession() : null;
		this.jcrKeyring = jcrKeyring;
		this.repositoryRegister = repositoryRegister;
		this.repositoryFactory = repositoryFactory;
		this.sortChildren = sortChildren;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null)// dispose
			return;

		if (userSession != null) {
			Node userHome = ArgeoJcrUtils.getUserHome(userSession);
			if (userHome != null) {
				// TODO : find a way to dynamically get alias for the node
				if (homeNode != null)
					homeNode.dispose();
				homeNode = new SingleJcrNode(null, userHome,
						userSession.getUserID(), ArgeoJcrConstants.ALIAS_NODE);
			}
		}
		if (repositoryRegister != null) {
			if (repositoriesNode != null)
				repositoriesNode.dispose();
			repositoriesNode = new RepositoriesNode("Repositories",
					repositoryRegister, repositoryFactory, null, jcrKeyring);
		}
	}

	/**
	 * Sends back the first level of the Tree. Independent from inputElement
	 * that can be null
	 */
	public Object[] getElements(Object inputElement) {
		List<Object> objs = new ArrayList<Object>();
		if (homeNode != null)
			objs.add(homeNode);
		if (repositoriesNode != null)
			objs.add(repositoriesNode);
		return objs.toArray();
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TreeParent) {
			if (sortChildren) {
				// TreeParent[] arr = (TreeParent[]) ((TreeParent)
				// parentElement)
				// .getChildren();
				Object[] tmpArr = ((TreeParent) parentElement).getChildren();
				TreeParent[] arr = new TreeParent[tmpArr.length];
				for (int i = 0; i < tmpArr.length; i++)
					arr[i] = (TreeParent) tmpArr[i];

				Arrays.sort(arr, itemComparator);
				return arr;
			} else
				return ((TreeParent) parentElement).getChildren();

		} else {
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
		if (element instanceof RepositoriesNode) {
			RepositoryRegister rr = ((RepositoriesNode) element)
					.getRepositoryRegister();
			return rr.getRepositories().size() > 0;
		} else if (element instanceof TreeParent) {
			TreeParent tp = (TreeParent) element;
			return tp.hasChildren();
		}
		return false;
	}

	public void dispose() {
		if (homeNode != null)
			homeNode.dispose();
		if (repositoriesNode != null) {
			// logs out open sessions
			// see https://bugzilla.argeo.org/show_bug.cgi?id=23
			repositoriesNode.dispose();
		}
	}

}
