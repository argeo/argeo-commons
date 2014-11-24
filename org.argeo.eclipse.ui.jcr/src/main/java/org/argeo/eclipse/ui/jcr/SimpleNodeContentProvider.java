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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;

/** Simple JCR node content provider taking a list of String as base path. */
public class SimpleNodeContentProvider extends AbstractNodeContentProvider {
	private final List<String> basePaths;
	private Boolean mkdirs = false;

	public SimpleNodeContentProvider(Session session, String... basePaths) {
		this(session, Arrays.asList(basePaths));
	}

	public SimpleNodeContentProvider(Session session, List<String> basePaths) {
		super(session);
		this.basePaths = basePaths;
	}

	@Override
	protected Boolean isBasePath(String path) {
		if (basePaths.contains(path))
			return true;
		return super.isBasePath(path);
	}

	public Object[] getElements(Object inputElement) {
		try {
			List<Node> baseNodes = new ArrayList<Node>();
			for (String basePath : basePaths)
				if (mkdirs && !getSession().itemExists(basePath))
					baseNodes.add(JcrUtils.mkdirs(getSession(), basePath));
				else
					baseNodes.add(getSession().getNode(basePath));
			return baseNodes.toArray();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get base nodes for " + basePaths,
					e);
		}
	}

	public List<String> getBasePaths() {
		return basePaths;
	}

	public void setMkdirs(Boolean mkdirs) {
		this.mkdirs = mkdirs;
	}

}
