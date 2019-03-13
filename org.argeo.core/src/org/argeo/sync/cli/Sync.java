package org.argeo.sync.cli;

import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.argeo.sync.fs.PathSync;

public class Sync {

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("r", "recursive", false, "recurse into directories");
		options.addOption(Option.builder().longOpt("progress").hasArg(false).desc("show progress").build());

		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(options, args);
			List<String> remaining = line.getArgList();
			if (remaining.size() == 0) {
				System.err.println("There must be at least one argument");
				printHelp(options);
				System.exit(1);
			}
			URI sourceUri = new URI(remaining.get(0));
			URI targetUri;
			if (remaining.size() == 1) {
				targetUri = Paths.get(System.getProperty("user.dir")).toUri();
			} else {
				targetUri = new URI(remaining.get(1));
			}
			PathSync pathSync = new PathSync(sourceUri, targetUri);
			pathSync.run();
		} catch (Exception exp) {
			exp.printStackTrace();
			printHelp(options);
			System.exit(1);
		}
	}

	public static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("sync SRC [DEST]", options, true);
	}

}
