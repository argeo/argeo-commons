package org.argeo.api.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.TreeMap;
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

	final static String _DESCRIPTION = "_description";
	final static String _USAGE = "_usage";
	final static String _EXAMPLE = "_example";

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

	public static <T extends Enum<T>> void printHelp(Class<T> clss, StringWriter out) {
		ResourceBundle rb = loadResourceBundle(clss, Locale.getDefault());
		EnumSet<T> names = EnumSet.allOf(clss);
		for (T e : names) {
			String optName = CLineParser.toOptName(e);
			if (rb.containsKey(optName)) {
				String desc = rb.getString(optName);
				out.append(optName + "\t" + desc + "\n");
			}
		}
	}

	private static <T extends Enum<T>> void addOptions(Locale locale, SortedMap<String, String> allOptions,
			ResourceBundle commandRb, Class<T> optionEnumClass) {
		ResourceBundle rb = loadResourceBundle(optionEnumClass, locale);
		EnumSet<T> names = EnumSet.allOf(optionEnumClass);
		for (T e : names) {
			String optName = CLineParser.toOptName(e);
			String desc;
			if (commandRb.containsKey(optName))
				desc = rb.getString(optName);
			else if (rb.containsKey(optName))
				desc = rb.getString(optName);
			else
				desc = optName.replace('-', ' ');
			allOptions.put(optName, desc);
		}
	}

	public static ResourceBundle loadResourceBundle(Class<?> clss, Locale locale) {
		ClassLoader classLoader = clss.getClassLoader();
		String resource = clss.getName();
		ResourceBundle rb = ResourceBundle.getBundle(resource, locale, classLoader);
		return rb;
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void printHelp(Writer out, Locale locale, Class commandClass, List<Class> commandOptions) {
		if (locale == null)
			locale = Locale.getDefault();

		ResourceBundle commandRb = ResourceBundle.getBundle(commandClass.getName(), locale,
				commandClass.getClassLoader());

		SortedMap<String, String> allOptions = new TreeMap<>();
		for (Class optionClass : commandOptions) {
			if (optionClass.isEnum()) {
				addOptions(locale, allOptions, commandRb, optionClass);
			}
		}

		String description = commandRb.containsKey(_DESCRIPTION) ? commandRb.getString(_DESCRIPTION) : "";
		List<String> usages = new ArrayList<>();
		for (int i = 0; i < 32; i++) {
			String key = i == 0 ? _USAGE : _USAGE + "." + i;
			if (commandRb.containsKey(key))
				usages.add(commandRb.getString(key));
		}
		List<String> examples = new ArrayList<>();
		for (int i = 0; i < 32; i++) {
			String key = i == 0 ? _EXAMPLE : _EXAMPLE + "." + i;
			if (commandRb.containsKey(key))
				examples.add(commandRb.getString(key));
		}

		// write it
		try {
			// TODO wrap?
			String prefix = "java " + commandClass.getName() + " ";
			for (String usage : usages)
				out.write(prefix + usage + "\n");
			out.write('\n');
			out.write(description);
			out.write('\n');
			out.write('\n');
			// options
			String leftPad = spaces(helpLeftPad);
			for (String opt : allOptions.keySet()) {
				out.write(leftPad);
				String optStr = "--" + opt;
				out.write(optStr);
				out.append(spaces(helpDescPad - optStr.length()));
				out.write(allOptions.get(opt));
				// TODO append valued option info
				out.write('\n');
			}
			out.write('\n');
			for (String example : examples)
				out.write(example + "\n");
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot write help for " + commandClass, e);
		}
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
