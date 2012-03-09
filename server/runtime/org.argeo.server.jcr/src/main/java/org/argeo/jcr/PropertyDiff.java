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
package org.argeo.jcr;

import javax.jcr.Value;

import org.argeo.ArgeoException;

/** The result of the comparison of two JCR properties. */
public class PropertyDiff {
	public final static Integer MODIFIED = 0;
	public final static Integer ADDED = 1;
	public final static Integer REMOVED = 2;

	private final Integer type;
	private final String relPath;
	private final Value referenceValue;
	private final Value newValue;

	public PropertyDiff(Integer type, String relPath, Value referenceValue,
			Value newValue) {
		super();

		if (type == MODIFIED) {
			if (referenceValue == null || newValue == null)
				throw new ArgeoException(
						"Reference and new values must be specified.");
		} else if (type == ADDED) {
			if (referenceValue != null || newValue == null)
				throw new ArgeoException(
						"New value and only it must be specified.");
		} else if (type == REMOVED) {
			if (referenceValue == null || newValue != null)
				throw new ArgeoException(
						"Reference value and only it must be specified.");
		} else {
			throw new ArgeoException("Unkown diff type " + type);
		}

		if (relPath == null)
			throw new ArgeoException("Relative path must be specified");

		this.type = type;
		this.relPath = relPath;
		this.referenceValue = referenceValue;
		this.newValue = newValue;
	}

	public Integer getType() {
		return type;
	}

	public String getRelPath() {
		return relPath;
	}

	public Value getReferenceValue() {
		return referenceValue;
	}

	public Value getNewValue() {
		return newValue;
	}

}
