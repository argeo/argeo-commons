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
