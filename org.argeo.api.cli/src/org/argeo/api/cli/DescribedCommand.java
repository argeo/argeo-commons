package org.argeo.api.cli;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** A command that can be described. */
public interface DescribedCommand<T> extends Function<List<String>, T> {
	default Options getOptions() {
		return new Options();
	}

	String getDescription();

	default String getUsage() {
		return null;
	}

	default String getExamples() {
		return null;
	}

	default CommandLine toCommandLine(List<String> args) {
		try {
			DefaultParser parser = new DefaultParser();
			return parser.parse(getOptions(), args.toArray(new String[args.size()]));
		} catch (ParseException e) {
			throw new CommandArgsException(e);
		}
	}

	/** In order to implement quickly a main method. */
	public static void mainImpl(DescribedCommand<?> command, String[] args) {
		try {
			Object output = command.apply(Arrays.asList(args));
			System.out.println(output);
			System.exit(0);
		} catch (PrintHelpRequestException e) {
			StringWriter out = new StringWriter();
			HelpCommand.printHelp(command, out);
			System.out.println(out.toString());
			System.exit(1);
		} catch (IllegalArgumentException e) {
			StringWriter out = new StringWriter();
			HelpCommand.printHelp(command, out);
			System.err.println(out.toString());
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
