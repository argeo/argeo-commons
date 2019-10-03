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
package org.argeo.node.tabular;

import java.util.List;

/** Minimal tabular row wrapping an {@link Object} array */
public class ArrayTabularRow implements TabularRow {
	private final Object[] arr;

	public ArrayTabularRow(List<?> objs) {
		this.arr = objs.toArray();
	}

	public Object get(Integer col) {
		return arr[col];
	}

	public int size() {
		return arr.length;
	}

	public Object[] toArray() {
		return arr;
	}

}
