package org.argeo.maintenance.backup.vfs;

import org.apache.commons.vfs2.FileObject;

/** Backups a PostgreSQL database using pg_dump. */
public class PostgreSqlBackup extends OsCallBackup {
	/**
	 * PostgreSQL password environment variable (see
	 * http://stackoverflow.com/questions
	 * /2893954/how-to-pass-in-password-to-pg-dump)
	 */
	protected final static String PGPASSWORD = "PGPASSWORD";

	private String pgDumpLocation = "/usr/bin/pg_dump";

	private String dbUser;
	private String dbPassword;
	private String dbName;

	public PostgreSqlBackup() {
		super();
	}

	public PostgreSqlBackup(String dbUser, String dbPassword, String dbName) {
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		this.dbName = dbName;
		init();
	}

	@Override
	public void init() {
		// disable compression since pg_dump is used with -Fc option
		setCompression(null);

		if (getName() == null)
			setName(dbName + ".pgdump");
		super.init();
	}

	@Override
	public void writeBackup(FileObject targetFo) {
		if (getCommand() == null) {
			getEnvironment().put(PGPASSWORD, dbPassword);
			setCommand(pgDumpLocation + " -Fc" + " -U ${dbUser} ${dbName}");
		}
		getVariables().put("dbUser", dbUser);
		getVariables().put("dbPassword", dbPassword);
		getVariables().put("dbName", dbName);

		super.writeBackup(targetFo);
	}

	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public void setPgDumpLocation(String mysqldumpLocation) {
		this.pgDumpLocation = mysqldumpLocation;
	}

}
