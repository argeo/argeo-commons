package org.argeo.api.cli;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/** A special command that can describe {@link DescribedCommand}. */
public class HelpCommand implements DescribedCommand<String> {
	/**
	 * System property forcing the root command to this value (typically the name of
	 * a script).
	 */
	public final static String ROOT_COMMAND_PROPERTY = "org.argeo.api.cli.rootCommand";

	final static String HELP = "help";
	final static Option HELP_OPTION = Option.builder().longOpt(HELP).desc("print this help").build();

	private CommandsCli commandsCli;
	private CommandsCli parentCommandsCli;

	// Help formatting
	private static int helpWidth = 80;
	private static int helpLeftPad = 4;
	private static int helpDescPad = 20;

	public HelpCommand(CommandsCli parentCommandsCli, CommandsCli commandsCli) {
		super();
		this.parentCommandsCli = parentCommandsCli;
		this.commandsCli = commandsCli;
	}

	@Override
	public String apply(List<String> args) {
		StringWriter out = new StringWriter();

		if (args.size() == 0) {// overview
			printHelp(commandsCli, out);
		} else {
			String cmd = args.get(0);
			Function<List<String>, ?> function = commandsCli.getCommand(cmd);
			if (function == null)
				return "Command " + cmd + " not found.";
			Options options;
			String examples;
			DescribedCommand<?> command = null;
			if (function instanceof DescribedCommand) {
				command = (DescribedCommand<?>) function;
				options = command.getOptions();
				examples = command.getExamples();
			} else {
				options = new Options();
				examples = null;
			}
			String description = getShortDescription(function);
			String commandCall = getCommandUsage(cmd, command);
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(new PrintWriter(out), helpWidth, commandCall, description, options, helpLeftPad,
					helpDescPad, examples, false);
		}
		return out.toString();
	}

	private static String getShortDescription(Function<List<String>, ?> function) {
		if (function instanceof DescribedCommand) {
			return ((DescribedCommand<?>) function).getDescription();
		} else {
			return function.toString();
		}
	}

	public String getCommandUsage(String cmd, DescribedCommand<?> command) {
		String commandCall = getCommandCall(commandsCli) + " " + cmd;
		assert command != null;
		if (command != null && command.getUsage() != null) {
			commandCall = commandCall + " " + command.getUsage();
		}
		return commandCall;
	}

	@Override
	public String getDescription() {
		return "Shows this help or describes a command";
	}

	@Override
	public String getUsage() {
		return "[command]";
	}

	public CommandsCli getParentCommandsCli() {
		return parentCommandsCli;
	}

	protected String getCommandCall(CommandsCli commandsCli) {
		HelpCommand hc = commandsCli.getHelpCommand();
		if (hc.getParentCommandsCli() != null) {
			return getCommandCall(hc.getParentCommandsCli()) + " " + commandsCli.getCommandName();
		} else {
			String rootCommand = System.getProperty(ROOT_COMMAND_PROPERTY);
			if (rootCommand != null)
				return rootCommand;
			return commandsCli.getCommandName();
		}
	}

	public static void printHelp(DescribedCommand<?> command, StringWriter out) {
		String usage = "java " + command.getClass().getName()
				+ (command.getUsage() != null ? " " + command.getUsage() : "");
		HelpFormatter formatter = new HelpFormatter();
		Options options = command.getOptions();
		options.addOption(HelpCommand.HELP_OPTION);
		formatter.printHelp(new PrintWriter(out), helpWidth, usage, command.getDescription(), options, helpLeftPad,
				helpDescPad, command.getExamples(), false);

	}

	public static void printHelp(CommandsCli commandsCli, String commandName, StringWriter out) {
		if (commandName == null) {
			printHelp(commandsCli, out);
			return;
		}
		DescribedCommand<?> command = (DescribedCommand<?>) commandsCli.getCommand(commandName);
		String usage = commandsCli.getHelpCommand().getCommandUsage(commandName, command);
		HelpFormatter formatter = new HelpFormatter();
		Options options = command.getOptions();
		options.addOption(HelpCommand.HELP_OPTION);
		formatter.printHelp(new PrintWriter(out), helpWidth, usage, command.getDescription(), options, helpLeftPad,
				helpDescPad, command.getExamples(), false);

	}

	public static void printHelp(CommandsCli commandsCli, StringWriter out) {
		out.append(commandsCli.getDescription()).append('\n');
		String leftPad = spaces(helpLeftPad);
		for (String cmd : commandsCli.getSubCommands()) {
			Function<List<String>, ?> function = commandsCli.getCommand(cmd);
			assert function != null;
			out.append(leftPad);
			out.append(cmd);
			// TODO deal with long commands
			out.append(spaces(helpDescPad - cmd.length()));
			out.append(getShortDescription(function));
			out.append('\n');
		}
	}

	private static String spaces(int count) {
		// Java 11
		// return " ".repeat(count);
		if (count <= 0)
			return "";
		else {
			StringBuilder sb = new StringBuilder(count);
			for (int i = 0; i < count; i++)
				sb.append(' ');
			return sb.toString();
		}
	}
}
