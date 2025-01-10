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

	@SafeVarargs
	public CLineParser(Class<? extends Enum<?>>... optEnums) {
		for (Class<? extends Enum<?>> clss : optEnums) {
			if (this.optEnums.contains(clss))
				throw new IllegalStateException("Options enum " + clss + " already added");
			this.optEnums.add(clss);
		}
	}

	public CLine parse(String... args) {
		return parse(Arrays.asList(args));
	}

	public CLine parse(List<String> args) {
		CLine cLine = new CLine(optEnums);

		Optional<Enum<?>> currOpt = null;
		Iterator<String> it = args.iterator();
		while (it.hasNext()) {
			String arg = it.next();

			Optional<Enum<?>> opt = null;
			// we force unchecked conversion
			for (@SuppressWarnings("rawtypes")
			Class optClass : optEnums) {
				@SuppressWarnings("unchecked")
				Optional<Enum<?>> o = findOpt(optClass, arg);
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
				currOpt = opt;
			} else if (currOpt != null && opt == null) {
				if (currOpt.get() instanceof ValuedOpt valuedOpt && valuedOpt.hasValue()) {// option value
					// TODO use namespace context
					Object value = CrAttributeType.parse(arg);
					cLine.put(currOpt.get(), value);
				} else {// plain argument
					cLine.put(currOpt.get(), Boolean.TRUE);
					cLine.addPlainArg(arg);
				}
				currOpt = null;
			} else if (currOpt != null && opt != null) {
				if (currOpt.get() instanceof ValuedOpt valuedOpt && valuedOpt.hasValue()) {// option with default
																							// value
					cLine.put(currOpt.get(), valuedOpt.defaultValue());
				} else {// option without value
					cLine.put(currOpt.get(), Boolean.TRUE);
				}
				currOpt = opt;
			}
		}
		return cLine;
	}

	private <T extends Enum<T>> Optional<T> findOpt(Class<T> optClass, String arg) {
		Optional<T> opt = null;
		if (arg.startsWith("--")) {// long option
			// TODO deal with special value '--' ? e.g. for additional arguments
			String pName = arg.substring(2);
			opt = EnumSet.allOf(optClass).stream().filter((p) -> p.name().replace('_', '-').equals(pName)).findAny();
		} else if (arg.startsWith("-")) {// short option
			// TODO deal with special value '-' ? e.g. for stdin
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

	public static void main(String[] args) {
		CLineParser parserTest = new CLineParser(Test.class);
		CLine cLine = parserTest.parse("--test1", "--test2", "value2", "plainArg1", "plainArg2");
		System.out.println(cLine);

		CLineParser parserTestValued = new CLineParser(TestValued.class);
		CLine cLineValued = parserTestValued.parse("--test1", "--test2", "value2", "plainArg1", "plainArg2");
		System.out.println(cLineValued);

	}

	static enum Test {
		test1, test2;
	}

	static enum TestValued implements ValuedOpt {
		test1, test2(true);

		private final boolean hasValue;

		private TestValued() {
			this(false);
		}

		private TestValued(boolean hasValue) {
			this.hasValue = hasValue;
		}

		@Override
		public boolean hasValue() {
			return hasValue;
		}
	}
}
