package org.argeo.cms.cli.posix;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.argeo.api.cli.DescribedCommand;

public class Echo implements DescribedCommand<String> {

	@Override
	public Options getOptions() {
		Options options = new Options();
		options.addOption(Option.builder("n").desc("do not output the trailing newline").build());
		return options;
	}

	@Override
	public String getDescription() {
		return "Display a line of text";
	}

	@Override
	public String getUsage() {
		return "[STRING]...";
	}

	@Override
	public String apply(List<String> args) {
		CommandLine cl = toCommandLine(args);

		StringBuffer sb = new StringBuffer();
		for (String s : cl.getArgList()) {
			sb.append(s).append(' ');
		}

		if (cl.hasOption('n')) {
			sb.deleteCharAt(sb.length() - 1);
		} else {
			sb.setCharAt(sb.length() - 1, '\n');
		}
		return sb.toString();
	}

}
