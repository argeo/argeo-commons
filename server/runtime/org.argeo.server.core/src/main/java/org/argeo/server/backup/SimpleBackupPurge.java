package org.argeo.server.backup;

import java.text.DateFormat;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.Selectors;
import org.argeo.ArgeoException;
import org.joda.time.DateTime;
import org.joda.time.Period;

/** Simple backup purge which keeps backups only for a given number of days */
public class SimpleBackupPurge implements BackupPurge {
	private final static Log log = LogFactory.getLog(SimpleBackupPurge.class);

	private Integer daysKept = 30;

	@Override
	public void purge(FileSystemManager fileSystemManager, String base,
			String name, DateFormat dateFormat, FileSystemOptions opts) {
		try {
			DateTime nowDt = new DateTime();
			FileObject baseFo = fileSystemManager.resolveFile(
					base + '/' + name, opts);

			SortedMap<DateTime, FileObject> toDelete = new TreeMap<DateTime, FileObject>();
			int backupCount = 0;

			// make sure base dir exists
			baseFo.createFolder();

			// scan backups and list those which should be deleted
			for (FileObject backupFo : baseFo.getChildren()) {
				String backupName = backupFo.getName().getBaseName();
				Date backupDate = dateFormat.parse(backupName);
				backupCount++;

				DateTime backupDt = new DateTime(backupDate.getTime());
				Period sinceThen = new Period(backupDt, nowDt);
				int days = sinceThen.getDays();
				// int days = sinceThen.getMinutes();
				if (days > daysKept) {
					toDelete.put(backupDt, backupFo);
				}
			}

			if (toDelete.size() != 0 && toDelete.size() == backupCount) {
				// all backups would be deleted
				// but we want to keep at least one
				DateTime lastBackupDt = toDelete.firstKey();
				FileObject keptFo = toDelete.remove(lastBackupDt);
				log.warn("Backup " + keptFo
						+ " kept although it is older than " + daysKept
						+ " days.");
			}

			// delete old backups
			for (FileObject backupFo : toDelete.values()) {
				backupFo.delete(Selectors.SELECT_ALL);
				if (log.isDebugEnabled())
					log.debug("Deleted backup " + backupFo);
			}
		} catch (Exception e) {
			throw new ArgeoException("Could not purge previous backups", e);
		}

	}

}
