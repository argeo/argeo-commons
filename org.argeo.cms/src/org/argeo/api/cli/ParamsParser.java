package org.argeo.api.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParamsParser<P extends Enum<P>> {

	private final Class<P> paramClass;
	private final EnumSet<P> params;

	private final boolean supportsShortOpt;
	private final boolean supportsMultipleOpt;

	public ParamsParser(Class<P> clss) {
		paramClass = clss;
		params = EnumSet.allOf(clss);
		supportsShortOpt = ShortOpt.class.isAssignableFrom(clss);
		supportsMultipleOpt = MultipleOpt.class.isAssignableFrom(clss);
	}

	public CLine<P> parse(String... args) {
		return parse(Arrays.asList(args));
	}

	public CLine<P> parse(List<String> args) {
		CLine<P> cLine = new CLine<>(paramClass);
//		List<String> plainArgs = new ArrayList<>();
		Optional<P> currParam = null;
		Iterator<String> it = args.iterator();
		args: while (it.hasNext()) {
			String arg = it.next();
			Optional<P> param = null;
			if (arg.startsWith("--")) {// long option
				// TODO deal with special value '--' ? e.g. for additional arguments
				String pName = arg.substring(2);
				param = params.stream().filter((p) -> p.name().replace('_', '-').equals(pName)).findAny();
			} else if (arg.startsWith("-")) {// short option
				// TODO deal with special value '-' ? e.g. for stdin
				if (!supportsShortOpt)
					throw new CommandArgsException(paramClass + " does not support short options");
				if (arg.length() != 2)
					throw new CommandArgsException("Short option " + arg + " has invalid length");
				char c = arg.charAt(1);
				param = params.stream().filter((p) -> {
					return c == ((ShortOpt) p).shortOpt();
				}).findAny();
			}

			if (param != null && param.isEmpty())
				throw new CommandArgsException("Unsupported option " + arg);
			if (currParam == null && param == null) {// plain argument
				cLine.addArg(arg);
			} else if (currParam == null && param != null) {// next option
				currParam = param;
			} else if (currParam != null && param == null) {// option value
				// TODO convert
				cLine.put(currParam.get(), arg);
				currParam = null;
			} else if (currParam != null && param != null) {// option without value
				cLine.put(currParam.get(), Boolean.TRUE);
				currParam = param;
			}
		}
		return cLine;
	}

	public static void main(String[] args) {
		ParamsParser<Test> parser = new ParamsParser<>(Test.class);
		parser.parse("--test1", "--test2", "value2", "value");

	}

	static enum Test {
		test1, test2;
	}
}
