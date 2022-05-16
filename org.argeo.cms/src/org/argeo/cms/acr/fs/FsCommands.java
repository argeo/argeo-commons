package org.argeo.cms.acr.fs;

import org.argeo.cms.cli.CommandsCli;

/** File utilities. */
public class FsCommands extends CommandsCli {

	public FsCommands(String commandName) {
		super(commandName);
		addCommand("sync", new FileSync());
	}

	@Override
	public String getDescription() {
		return "Utilities around files and file systems";
	}

}
