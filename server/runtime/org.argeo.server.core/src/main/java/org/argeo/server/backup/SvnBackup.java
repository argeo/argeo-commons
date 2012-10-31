package org.argeo.server.backup;

import java.io.File;

import org.apache.commons.vfs.FileObject;

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
