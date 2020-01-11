package org.argeo.cli.posix;

import org.argeo.cli.CommandsCli;

/** POSIX commands. */
public class PosixCommands extends CommandsCli {

	public PosixCommands(String commandName) {
		super(commandName);
		addCommand("echo", new Echo());
	}

	@Override
	public String getDescription() {
		return "Reimplementation of some POSIX commands in plain Java";
	}

	public static void main(String[] args) {
		mainImpl(new PosixCommands("argeo-posix"), args);
	}
}
