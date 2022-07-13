package org.argeo.cms.cli;

import org.apache.commons.cli.Option;
import org.argeo.api.cli.CommandsCli;
import org.argeo.cms.cli.posix.PosixCommands;
import org.argeo.cms.ssh.cli.SshCli;

/** Argeo command line tools. */
public class ArgeoCli extends CommandsCli {
	public ArgeoCli(String commandName) {
		super(commandName);
		// Common options
		options.addOption(Option.builder("v").hasArg().argName("verbose").desc("verbosity").build());
		options.addOption(
				Option.builder("D").hasArgs().argName("property=value").desc("use value for given property").build());

		// common
		addCommandsCli(new SshCli("ssh"));
		addCommandsCli(new PosixCommands("posix"));
		addCommandsCli(new FsCommands("fs"));
	}

	@Override
	public String getDescription() {
		return "Argeo utilities";
	}

	public static void main(String[] args) {
		mainImpl(new ArgeoCli("argeo"), args);
	}

}
