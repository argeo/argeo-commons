package org.argeo.api.cli;

public class CommandArgsException extends IllegalArgumentException {
	private static final long serialVersionUID = -7271050747105253935L;
	private String commandName;
	private volatile CommandsCli commandsCli;

	public CommandArgsException(Exception cause) {
		super(cause.getMessage(), cause);
	}

	public CommandArgsException(String message) {
		super(message);
	}

	public String getCommandName() {
		return commandName;
	}

	public void setCommandName(String commandName) {
		this.commandName = commandName;
	}

	public CommandsCli getCommandsCli() {
		return commandsCli;
	}

	public void setCommandsCli(CommandsCli commandsCli) {
		this.commandsCli = commandsCli;
	}

}
