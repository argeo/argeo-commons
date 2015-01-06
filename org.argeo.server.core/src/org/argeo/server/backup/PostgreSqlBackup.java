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
package org.argeo.server.backup;

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
