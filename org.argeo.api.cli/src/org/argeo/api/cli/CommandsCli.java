package org.argeo.api.cli;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** Base class for a CLI managing sub commands. */
public abstract class CommandsCli implements DescribedCommand<Object> {
	public final static String HELP = "help";

	private final String commandName;
	private Map<String, Function<List<String>, ?>> commands = new TreeMap<>();

	protected final Options options = new Options();

	public CommandsCli(String commandName) {
		this.commandName = commandName;
	}

	@Override
	public Object apply(List<String> args) {
		String cmd = null;
		List<String> newArgs = new ArrayList<>();
		try {
			CommandLineParser clParser = new DefaultParser();
			CommandLine commonCl = clParser.parse(getOptions(), args.toArray(new String[args.size()]), true);
			List<String> leftOvers = commonCl.getArgList();
			for (String arg : leftOvers) {
				if (!arg.startsWith("-") && cmd == null) {
					cmd = arg;
				} else {
					newArgs.add(arg);
				}
			}
		} catch (ParseException e) {
			CommandArgsException cae = new CommandArgsException(e);
			throw cae;
		}

		Function<List<String>, ?> function = cmd != null ? getCommand(cmd) : getDefaultCommand();
		if (function == null)
			throw new IllegalArgumentException("Uknown command " + cmd);
		try {
			Object value = function.apply(newArgs);
			return value != null ? value.toString() : null;
		} catch (CommandArgsException e) {
			if (e.getCommandName() == null) {
				e.setCommandName(cmd);
				e.setCommandsCli(this);
			}
			throw e;
		} catch (IllegalArgumentException e) {
			CommandArgsException cae = new CommandArgsException(e);
			cae.setCommandName(cmd);
			throw cae;
		}
	}

	@Override
	public Options getOptions() {
		return options;
	}

	protected void addCommand(String cmd, Function<List<String>, ?> function) {
		commands.put(cmd, function);

	}

	@Override
	public String getUsage() {
		return "[command]";
	}

	protected void addCommandsCli(CommandsCli commandsCli) {
		addCommand(commandsCli.getCommandName(), commandsCli);
		commandsCli.addCommand(HELP, new HelpCommand(this, commandsCli));
	}

	public String getCommandName() {
		return commandName;
	}

	public Set<String> getSubCommands() {
		return commands.keySet();
	}

	public Function<List<String>, ?> getCommand(String command) {
		return commands.get(command);
	}

	public HelpCommand getHelpCommand() {
		return (HelpCommand) getCommand(HELP);
	}

	public Function<List<String>, String> getDefaultCommand() {
		return getHelpCommand();
	}

	/** In order to implement quickly a main method. */
	public static void mainImpl(CommandsCli cli, String[] args) {
		try {
			cli.addCommand(CommandsCli.HELP, new HelpCommand(null, cli));
			Object output = cli.apply(Arrays.asList(args));
			System.out.println(output);
			System.exit(0);
		} catch (CommandArgsException e) {
			System.err.println("Wrong arguments " + Arrays.toString(args) + ": " + e.getMessage());
			Throwable cause = e.getCause();
			if (!(cause instanceof MissingOptionException))
				e.printStackTrace();
			if (e.getCommandName() != null) {
				StringWriter out = new StringWriter();
				HelpCommand.printHelp(e.getCommandsCli(), e.getCommandName(), out);
				System.err.println(out.toString());
			} else {
				e.printStackTrace();
			}
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
