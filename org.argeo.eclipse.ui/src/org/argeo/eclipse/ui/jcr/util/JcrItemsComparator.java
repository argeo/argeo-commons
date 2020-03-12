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
package org.argeo.eclipse.ui.jcr.util;

import java.util.Comparator;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.argeo.eclipse.ui.EclipseUiException;

/** Compares two JCR items (node or properties) based on their names. */
public class JcrItemsComparator implements Comparator<Item> {
	public int compare(Item o1, Item o2) {
		try {
			// TODO: put folder before files
			return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot compare " + o1 + " and " + o2, e);
		}
	}

}
