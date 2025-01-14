package org.argeo.api.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.argeo.api.acr.CrAttributeType;

public class CLineParser {

	private final List<Class<? extends Enum<?>>> optEnums = new ArrayList<>();

	private final static String DOUBLE_DASH = "--";
	private final static String SINGLE_DASH = "-";
	private final static char EQU = '=';

	@SuppressWarnings({ "unchecked" })
	public CLineParser(List<Class<? extends Enum<?>>> classes) {
		this(classes.toArray(new Class[0]));
	}

	@SafeVarargs
	public CLineParser(Class<? extends Enum<?>>... optEnums) throws IllegalArgumentException {
		for (Class<? extends Enum<?>> clss : optEnums) {
			if (this.optEnums.contains(clss))
				throw new IllegalArgumentException("Options enum " + clss + " added multiple times");
			this.optEnums.add(clss);
		}

		// always add help
		if (!this.optEnums.contains(HelpOpt.class))
			this.optEnums.add(HelpOpt.class);
	}

	public CLine parse(String... args) throws CommandArgsException {
		return parse(Arrays.asList(args));
	}

	public CLine parse(List<String> args) throws CommandArgsException {
		return parseImpl(optEnums, args, null);
	}

	static CLineImpl parseImpl(List<Class<? extends Enum<?>>> optEnums, List<String> args, CommandsCli commandsCli)
			throws CommandArgsException {
		if (!optEnums.contains(HelpOpt.class))
			optEnums.add(HelpOpt.class);
		CLineImpl cLine = new CLineImpl(optEnums);

		Optional<Enum<?>> currOpt = null;
		Iterator<String> argIterator = args.iterator();
		while (argIterator.hasNext()) {
			currOpt = process(commandsCli, cLine, argIterator, currOpt);
		}
		return cLine;
	}

	private static Optional<Enum<?>> process(CommandsCli commandsCli, CLineImpl cLine, Iterator<String> argIterator,
			Optional<Enum<?>> currOpt) {
		assert argIterator.hasNext();

		Optional<Enum<?>> nextCurrOpt;
		String arg = argIterator.next();

		StringBuilder equValue = new StringBuilder();
		Optional<Enum<?>> opt = findOpt(cLine.getOptEnums(), arg, equValue);

		if (opt != null && opt.isEmpty())
			throw new CommandArgsException("Unsupported option " + arg);

		if (currOpt == null) {
			if (opt == null) {// plain argument
				addPlainArgOrSubCommand(commandsCli, cLine, argIterator, arg);
				nextCurrOpt = null;
			} else {// next option
				String v = equValue.toString();
				if ("".equals(v)) {
					nextCurrOpt = opt;
				} else {
					cLine.put(opt.get(), CrAttributeType.parse(v));
					nextCurrOpt = null;
				}
			}
		} else {
			if (opt == null) {
				if (currOpt.get() instanceof ValuedOpt valuedOpt //
						&& !ValuedOpt.isFlag(valuedOpt)) {// option value
					// TODO use namespace context
					cLine.put(currOpt.get(), CrAttributeType.parse(arg));
				} else {// plain argument
					cLine.put(currOpt.get(), Boolean.TRUE);
					addPlainArgOrSubCommand(commandsCli, cLine, argIterator, arg);
				}
				nextCurrOpt = null;
			} else {
				if (currOpt.get() instanceof ValuedOpt valuedOpt //
						&& !ValuedOpt.isFlag(valuedOpt)) {// option with default value
					Object defaultValue = valuedOpt.defaultValue();
					if (defaultValue == null)
						throw new CommandArgsException(currOpt.get() + " must have an explicit value");
					cLine.put(currOpt.get(), defaultValue);
				} else {// flag
					cLine.put(currOpt.get(), Boolean.TRUE);
				}
				nextCurrOpt = opt;
			}
		}
		return nextCurrOpt;
	}

	@SuppressWarnings("unchecked")
	private static void addPlainArgOrSubCommand(CommandsCli commandsCli, CLineImpl cLine, Iterator<String> argIterator,
			String arg) {
		if (commandsCli == null) {
			cLine.addPlainArg(arg);
			return;
		}

		assert cLine.getPlainArgs().isEmpty() : "Plain arguments must be after subcommand";
		if (!commandsCli.getSubCommands().contains(arg))
			throw new CommandArgsException(arg + " is not a subcommand");
		Function<List<String>, ?> command = commandsCli.getCommand(arg);

		if (command instanceof DescribedCommand describedCommand) {
			cLine.registerOptEnums(describedCommand.getOptClasses());
		}

		// recursively process subcommands
		CommandsCli cc = null;
		if (command instanceof CommandsCli)
			cc = (CommandsCli) command;
		else
			cLine.setCommand(command);

		Optional<Enum<?>> currOpt = null;
		while (argIterator.hasNext()) {
			currOpt = process(cc, cLine, argIterator, currOpt);
		}

		if (cLine.getCommand() == null)
			throw new CommandArgsException("No executable subcommand was defined");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Optional<Enum<?>> findOpt(Set<Class<? extends Enum<?>>> optEnums, String arg,
			StringBuilder equValue) {
		Optional<Enum<?>> opt = null;
		// we force unchecked conversion
		for (Class optClass : optEnums) {
			equValue.setLength(0);// reset
			Optional<Enum<?>> o = findOpt(optClass, arg, equValue);
			if (o != null && !o.isEmpty()) {
				if (opt != null)
					throw new IllegalStateException("More than one option enum found for arg " + arg + ": "
							+ opt.getClass() + " and " + o.getClass());
				opt = o;
				// we continue in order to detect multiple option enums
			}
		}
		return opt;
	}

	private static <T extends Enum<T>> Optional<T> findOpt(Class<T> optClass, String arg, StringBuilder value)
			throws CommandArgsException {
		Optional<T> opt = null;
		// TODO ? deal with -D Java system properties
		// TODO use prefix / namespaces
		if (arg.startsWith(DOUBLE_DASH)) {// long option
			// TODO ? deal with special value '--', e.g. for additional arguments
			String raw = arg.substring(2);
			int equIndex = raw.indexOf(EQU);
			String pName;
			if (equIndex > -1) {
				pName = raw.substring(0, equIndex);
				value.append(raw.substring(equIndex + 1));
			} else {
				pName = raw;
			}
			opt = EnumSet.allOf(optClass).stream().filter((p) -> toOptName(p).equals(pName)).findAny();
		} else if (arg.startsWith(SINGLE_DASH)) {// short option
			// TODO ? deal with special value '-', e.g. for stdin
			if (!ShortOpt.class.isAssignableFrom(optClass))
				return null;
//				throw new CommandArgsException(optClass + " does not support short options");
			if (arg.length() != 2)
				throw new CommandArgsException("Short option " + arg + " has invalid length");
			char c = arg.charAt(1);
			opt = EnumSet.allOf(optClass).stream().filter((p) -> {
				return c == ((ShortOpt) p).shortOpt();
			}).findAny();
		}
		return opt;
	}

	static String toOptName(Enum<?> p) {
		return p.name().replace('_', '-');
	}

	static String toEnumName(String optName) {
		return optName.replace('-', '_');
	}
}
