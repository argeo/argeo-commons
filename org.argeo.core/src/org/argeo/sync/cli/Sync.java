package org.argeo.sync.cli;

import java.net.URI;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.argeo.sync.fs.PathSync;

public class Sync {

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("r", "recursive", false, "recurse into directories");
		options.addOption(Option.builder().longOpt("progress").hasArg(false).desc("").build());

		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(options, args);
			List<String> remaining = line.getArgList();

			URI sourceUri = new URI(remaining.get(0));
			URI targetUri = new URI(remaining.get(1));
			PathSync pathSync = new PathSync(sourceUri, targetUri);
			pathSync.run();
		} catch (Exception exp) {
			exp.printStackTrace();

		}
	}

}
