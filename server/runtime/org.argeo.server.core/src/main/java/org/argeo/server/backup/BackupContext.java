package org.argeo.server.backup;

import java.text.DateFormat;
import java.util.Date;

/**
 * Transient information of a given backup, centralizing common information such
 * as timestamp and location.
 */
public interface BackupContext {
	/** Backup date */
	public Date getTimestamp();

	/** Formatted backup date */
	public String getTimestampAsString();

	/** System name */
	public String getSystemName();

	/** Local base */
	public String getRelativeFolder();

	/** Date format */
	public DateFormat getDateFormat();
}
