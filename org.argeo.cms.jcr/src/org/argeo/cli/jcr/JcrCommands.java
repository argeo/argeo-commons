package org.argeo.cli.jcr;

import org.argeo.cli.CommandsCli;

/** File utilities. */
public class JcrCommands extends CommandsCli {

	public JcrCommands(String commandName) {
		super(commandName);
		addCommand("sync", new JcrSync());
	}

	@Override
	public String getDescription() {
		return "Utilities around remote and local JCR repositories";
	}

}
