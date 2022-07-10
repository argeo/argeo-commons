package org.argeo.cms.ssh.cli;

import org.argeo.api.cli.CommandsCli;

public class SshCli extends CommandsCli {
	public SshCli(String commandName) {
		super(commandName);
		addCommand("shell", new SshShell());
	}

	@Override
	public String getDescription() {
		return "SSH utilities.";
	}

	 
}
