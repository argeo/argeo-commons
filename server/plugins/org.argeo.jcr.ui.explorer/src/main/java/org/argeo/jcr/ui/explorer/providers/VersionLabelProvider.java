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
package org.argeo.jcr.ui.explorer.providers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.argeo.ArgeoException;
import org.eclipse.jface.viewers.ColumnLabelProvider;

/**
 * simple wrapping of the ColumnLabelProvider class to provide text display in
 * order to build a tree for version. The Get text method does not assume that
 * Version extends Node class to respect JCR 2.0 specification
 * 
 */
public class VersionLabelProvider extends ColumnLabelProvider {

	public String getText(Object element) {
		try {
			if (element instanceof Version) {
				Version version = (Version) element;
				return version.getName();
			} else if (element instanceof Node) {
				return ((Node) element).getName();
			}
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while getting element name", re);
		}
		return super.getText(element);
	}
}
