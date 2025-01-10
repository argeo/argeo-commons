package org.argeo.api.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.argeo.api.acr.CrAttributeType;

public class CLineParser {

	private final List<Class<? extends Enum<?>>> optEnums = new ArrayList<>();

	private final static String DOUBLE_DASH = "--";
	private final static String SINGLE_DASH = "--";
	private final static char EQU = '=';

	@SafeVarargs
	public CLineParser(Class<? extends Enum<?>>... optEnums) throws IllegalArgumentException {
		for (Class<? extends Enum<?>> clss : optEnums) {
			if (this.optEnums.contains(clss))
				throw new IllegalArgumentException("Options enum " + clss + " added multiple times");
			this.optEnums.add(clss);
		}
	}

	public CLine parse(String... args) throws CommandArgsException {
		return parse(Arrays.asList(args));
	}

	public CLine parse(List<String> args) throws CommandArgsException {
		CLineImpl cLine = new CLineImpl(optEnums);

		Optional<Enum<?>> currOpt = null;
		Iterator<String> it = args.iterator();
		while (it.hasNext()) {
			String arg = it.next();

			Optional<Enum<?>> opt = null;
			// value if of the form --opt=value
			StringBuilder equValue = null;
			// we force unchecked conversion
			for (@SuppressWarnings("rawtypes")
			Class optClass : optEnums) {
				equValue = new StringBuilder();
				@SuppressWarnings("unchecked")
				Optional<Enum<?>> o = findOpt(optClass, arg, equValue);
				if (o != null) {
					if (opt != null)
						throw new IllegalStateException("More than one option enum found for arg " + arg + ": "
								+ opt.getClass() + " and " + o.getClass());
					opt = o;
					// we continue in order to detect multiple option enums
				}
			}

			if (opt != null && opt.isEmpty())
				throw new CommandArgsException("Unsupported option " + arg);

			if (currOpt == null && opt == null) {// plain argument
				cLine.addPlainArg(arg);
			} else if (currOpt == null && opt != null) {// next option
				assert equValue != null;
				String v = equValue.toString();
				if ("".equals(v))
					currOpt = opt;
				else
					cLine.put(opt.get(), CrAttributeType.parse(v));
			} else if (currOpt != null && opt == null) {
				if (currOpt.get() instanceof ValuedOpt valuedOpt //
						&& !ValuedOpt.isFlag(valuedOpt)) {// option value
					// TODO use namespace context
					cLine.put(currOpt.get(), CrAttributeType.parse(arg));
				} else {// plain argument
					cLine.put(currOpt.get(), Boolean.TRUE);
					cLine.addPlainArg(arg);
				}
				currOpt = null;
			} else if (currOpt != null && opt != null) {
				if (currOpt.get() instanceof ValuedOpt valuedOpt //
						&& !ValuedOpt.isFlag(valuedOpt)) {// option with default value
					Object defaultValue = valuedOpt.defaultValue();
					if (defaultValue == null)
						throw new CommandArgsException(currOpt.get() + " must have an explicit value");
					cLine.put(currOpt.get(), defaultValue);
				} else {// flag
					cLine.put(currOpt.get(), Boolean.TRUE);
				}
				currOpt = opt;
			}
		}
		return cLine;
	}

	private <T extends Enum<T>> Optional<T> findOpt(Class<T> optClass, String arg, StringBuilder value)
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
				throw new CommandArgsException(optClass + " does not support short options");
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
