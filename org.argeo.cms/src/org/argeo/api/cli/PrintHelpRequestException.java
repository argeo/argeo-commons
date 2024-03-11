package org.argeo.api.cli;

/** An exception indicating that help should be printed. */
class PrintHelpRequestException extends RuntimeException {

	private static final long serialVersionUID = -9029122270660656639L;

	private String commandName;
	private volatile CommandsCli commandsCli;

	public PrintHelpRequestException(String commandName, CommandsCli commandsCli) {
		super();
		this.commandName = commandName;
		this.commandsCli = commandsCli;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getCommandName() {
		return commandName;
	}

	public CommandsCli getCommandsCli() {
		return commandsCli;
	}

}
