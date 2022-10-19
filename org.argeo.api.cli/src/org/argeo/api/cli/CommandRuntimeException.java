package org.argeo.api.cli;

import java.util.List;

/** {@link RuntimeException} referring during a command run. */
public class CommandRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 5595999301269377128L;

	private final DescribedCommand<?> command;
	private final List<String> arguments;

	public CommandRuntimeException(Throwable e, DescribedCommand<?> command, List<String> arguments) {
		this(null, e, command, arguments);
	}

	public CommandRuntimeException(String message, DescribedCommand<?> command, List<String> arguments) {
		this(message, null, command, arguments);
	}

	public CommandRuntimeException(String message, Throwable e, DescribedCommand<?> command, List<String> arguments) {
		super(message == null ? "(" + command.getClass().getName() + " " + arguments.toString() + ")"
				: message + " (" + command.getClass().getName() + " " + arguments.toString() + ")", e);
		this.command = command;
		this.arguments = arguments;
	}

	public DescribedCommand<?> getCommand() {
		return command;
	}

	public List<String> getArguments() {
		return arguments;
	}

}
