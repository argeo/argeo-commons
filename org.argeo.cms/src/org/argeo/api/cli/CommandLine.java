package org.argeo.api.cli;

import java.util.ArrayList;
import java.util.List;

/** For transition from Apache Exec. */
class CommandLine {
	/** The actual OS arguments, that it index 0 is the executable itself. */
	private final List<String> osArgs;

	CommandLine(List<String> osArgs) {
		// copy
		this.osArgs = new ArrayList<>(osArgs);
	}

	void addArgument(String arg) {
		osArgs.add(arg);
	}
	
	ProcessBuilder toProcessBuilder() {
		return new ProcessBuilder(osArgs);
	}

	static CommandLine parse(CharSequence cmd) {
		List<String> osArgs = new ArrayList<>();
		// FIXME deal with " and '
		StringBuilder currentArg = new StringBuilder();
		chars: for (int i = 0; i < cmd.length(); i++) {
			char c = cmd.charAt(i);
			if (' ' == c) {
				if (currentArg.length() == 0)
					continue chars;
				osArgs.add(currentArg.toString());
				currentArg = new StringBuilder();
				continue chars;
			}
			currentArg.append(c);
		}
		return new CommandLine(osArgs);
	}
}
