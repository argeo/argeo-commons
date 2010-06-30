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

package org.argeo.server.jxl.dao;

public class SimpleObject {
	private String string;
	private Integer integer;
	private OtherObject otherObject;
	private OtherObject anotherObject;

	public String getString() {
		return string;
	}

	public void setString(String sting) {
		this.string = sting;
	}

	public Integer getInteger() {
		return integer;
	}

	public void setInteger(Integer integer) {
		this.integer = integer;
	}

	public OtherObject getOtherObject() {
		return otherObject;
	}

	public void setOtherObject(OtherObject otherObject) {
		this.otherObject = otherObject;
	}

	public OtherObject getAnotherObject() {
		return anotherObject;
	}

	public void setAnotherObject(OtherObject anotherObject) {
		this.anotherObject = anotherObject;
	}

	@Override
	public boolean equals(Object obj) {
		return string.equals(((SimpleObject) obj).string);
	}

	@Override
	public int hashCode() {
		return string.hashCode();
	}

}
