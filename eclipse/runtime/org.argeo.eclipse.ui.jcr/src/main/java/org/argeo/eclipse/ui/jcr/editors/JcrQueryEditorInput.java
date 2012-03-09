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
package org.argeo.eclipse.ui.jcr.editors;

import javax.jcr.query.Query;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class JcrQueryEditorInput implements IEditorInput {
	private final String query;
	private final String queryType;

	public JcrQueryEditorInput(String query, String queryType) {
		this.query = query;
		if (queryType == null)
			this.queryType = Query.JCR_SQL2;
		else
			this.queryType = queryType;
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	public boolean exists() {
		return true;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return query;
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return query;
	}

	public String getQuery() {
		return query;
	}

	public String getQueryType() {
		return queryType;
	}

}
