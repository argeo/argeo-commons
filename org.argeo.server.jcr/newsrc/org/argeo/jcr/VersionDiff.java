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
package org.argeo.jcr;

import java.util.Calendar;
import java.util.Map;

/**
 * Generic Object that enables the creation of history reports based on a JCR
 * versionable node. userId and creation date are added to the map of
 * PropertyDiff.
 * 
 * These two fields might be null
 * 
 */
public class VersionDiff {

	private String userId;
	private Map<String, PropertyDiff> diffs;
	private Calendar updateTime;

	public VersionDiff(String userId, Calendar updateTime,
			Map<String, PropertyDiff> diffs) {
		this.userId = userId;
		this.updateTime = updateTime;
		this.diffs = diffs;
	}

	public String getUserId() {
		return userId;
	}

	public Map<String, PropertyDiff> getDiffs() {
		return diffs;
	}

	public Calendar getUpdateTime() {
		return updateTime;
	}
}
