package org.argeo.cms.ssh.cli;

import org.argeo.api.cli.CommandsCli;

/** SSH command line interface. */
public class SshCli extends CommandsCli {
	public SshCli(String commandName) {
		super(commandName);
		addCommand("shell", new SshShell());
	}

	@Override
	public String getDescription() {
		return "SSH utilities.";
	}

	public static void main(String[] args) {
		mainImpl(new SshCli("ssh"), args);
	}

}
