package org.argeo.server.backup;

import org.apache.commons.vfs.FileObject;

/** Backups a MySQL database using mysqldump */
public class MySqlBackup extends OsCallBackup {
	private String mysqldumpLocation = "/usr/bin/mysqldump";

	private String dbUser;
	private String dbPassword;
	private String dbName;

	public MySqlBackup() {
		super();
	}

	public MySqlBackup(String dbUser, String dbPassword, String dbName) {
		super(dbName);
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		this.dbName = dbName;
	}

	@Override
	public void writeBackup(FileObject targetFo) {
		if (getCommand() == null)
			setCommand(mysqldumpLocation
					+ " --lock-tables --add-locks --add-drop-table"
					+ " -u ${dbUser} --password=${dbPassword} --databases ${dbName}");
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

	public void setMysqldumpLocation(String mysqldumpLocation) {
		this.mysqldumpLocation = mysqldumpLocation;
	}

}
