package org.argeo.cms.acr.fs;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.argeo.cms.cli.CommandArgsException;
import org.argeo.cms.cli.DescribedCommand;

public class FileSync implements DescribedCommand<SyncResult<Path>> {
	final static Option deleteOption = Option.builder().longOpt("delete").desc("delete from target").build();
	final static Option recursiveOption = Option.builder("r").longOpt("recursive").desc("recurse into directories")
			.build();
	final static Option progressOption = Option.builder().longOpt("progress").hasArg(false).desc("show progress")
			.build();

	@Override
	public SyncResult<Path> apply(List<String> t) {
		try {
			CommandLine line = toCommandLine(t);
			List<String> remaining = line.getArgList();
			if (remaining.size() == 0) {
				throw new CommandArgsException("There must be at least one argument");
			}
			URI sourceUri = new URI(remaining.get(0));
			URI targetUri;
			if (remaining.size() == 1) {
				targetUri = Paths.get(System.getProperty("user.dir")).toUri();
			} else {
				targetUri = new URI(remaining.get(1));
			}
			boolean delete = line.hasOption(deleteOption.getLongOpt());
			boolean recursive = line.hasOption(recursiveOption.getLongOpt());
			PathSync pathSync = new PathSync(sourceUri, targetUri, delete, recursive);
			return pathSync.call();
		} catch (URISyntaxException e) {
			throw new CommandArgsException(e);
		}
	}

	@Override
	public Options getOptions() {
		Options options = new Options();
		options.addOption(recursiveOption);
		options.addOption(deleteOption);
		options.addOption(progressOption);
		return options;
	}

	@Override
	public String getUsage() {
		return "[source URI] [target URI]";
	}

	public static void main(String[] args) {
		DescribedCommand.mainImpl(new FileSync(), args);
//		Options options = new Options();
//		options.addOption("r", "recursive", false, "recurse into directories");
//		options.addOption(Option.builder().longOpt("progress").hasArg(false).desc("show progress").build());
//
//		CommandLineParser parser = new DefaultParser();
//		try {
//			CommandLine line = parser.parse(options, args);
//			List<String> remaining = line.getArgList();
//			if (remaining.size() == 0) {
//				System.err.println("There must be at least one argument");
//				printHelp(options);
//				System.exit(1);
//			}
//			URI sourceUri = new URI(remaining.get(0));
//			URI targetUri;
//			if (remaining.size() == 1) {
//				targetUri = Paths.get(System.getProperty("user.dir")).toUri();
//			} else {
//				targetUri = new URI(remaining.get(1));
//			}
//			PathSync pathSync = new PathSync(sourceUri, targetUri);
//			pathSync.run();
//		} catch (Exception exp) {
//			exp.printStackTrace();
//			printHelp(options);
//			System.exit(1);
//		}
	}

//	public static void printHelp(Options options) {
//		HelpFormatter formatter = new HelpFormatter();
//		formatter.printHelp("sync SRC [DEST]", options, true);
//	}

	@Override
	public String getDescription() {
		return "Synchronises files";
	}

}
