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

/** The column in a tabular content */
public class TabularColumn {
	private String name;
	/**
	 * JCR types, see
	 * http://www.day.com/maven/javax.jcr/javadocs/jcr-2.0/index.html
	 * ?javax/jcr/PropertyType.html
	 */
	private Integer type;

	/** column with default type */
	public TabularColumn(String name) {
		super();
		this.name = name;
	}

	public TabularColumn(String name, Integer type) {
		super();
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

}
