package org.argeo.jcr;

import java.util.Calendar;
import java.util.Map;

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
