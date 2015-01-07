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

import java.io.File;

import org.apache.commons.vfs2.FileObject;

/** Backups a Subversion repository using svnadmin. */
public class SvnBackup extends OsCallBackup {
	private String svnadminLocation = "/usr/bin/svnadmin";

	private String repoLocation;
	private String repoName;

	public SvnBackup() {
	}

	public SvnBackup(String repoLocation) {
		this.repoLocation = repoLocation;
		init();
	}

	@Override
	public void init() {
		// use directory as repo name
		if (repoName == null)
			repoName = new File(repoLocation).getName();

		if (getName() == null)
			setName(repoName + ".svndump");
		super.init();
	}

	@Override
	public void writeBackup(FileObject targetFo) {
		if (getCommand() == null) {
			setCommand(svnadminLocation + " dump " + " ${repoLocation}");
		}
		getVariables().put("repoLocation", repoLocation);

		super.writeBackup(targetFo);
	}

	public void setRepoLocation(String repoLocation) {
		this.repoLocation = repoLocation;
	}

	public void setRepoName(String repoName) {
		this.repoName = repoName;
	}

	public void setSvnadminLocation(String mysqldumpLocation) {
		this.svnadminLocation = mysqldumpLocation;
	}

}
