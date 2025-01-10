package org.argeo.cms.cli;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.argeo.api.cli.CLine;
import org.argeo.api.cli.CommandArgsException;
import org.argeo.api.cli.DescribedCommand;
import org.argeo.cms.file.PathSync;
import org.argeo.cms.file.SyncResult;

/** Synchronizes files between two directories. */
public class FileSync implements DescribedCommand<SyncResult<Path>> {
	private enum Opt {
		delete, //
		recursive, //
		progress, //
	}

//	final static Option deleteOption = Option.builder().longOpt("delete").desc("delete from target").build();
//	final static Option recursiveOption = Option.builder("r").longOpt("recursive").desc("recurse into directories")
//			.build();
//	final static Option progressOption = Option.builder().longOpt("progress").hasArg(false).desc("show progress")
//			.build();

	@Override
	public Class<? extends Enum<?>> getOptClass() {
		return Opt.class;
	}

	@Override
	public SyncResult<Path> apply(List<String> t) {
		try {
			// CommandLine line = toCommandLine(t);
			CLine line = toCLine(t);
//			List<String> remaining = line.getArgList();
			List<String> remaining = line.getPlainArgs();
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
//			boolean delete = line.hasOption(deleteOption.getLongOpt());
//			boolean recursive = line.hasOption(recursiveOption.getLongOpt());
			boolean delete = line.flag(Opt.delete);
			boolean recursive = line.flag(Opt.recursive);
			PathSync pathSync = new PathSync(sourceUri, targetUri, delete, recursive);
			return pathSync.call();
		} catch (URISyntaxException e) {
			throw new CommandArgsException(e);
		}
	}

//	@Override
//	public Options getOptions() {
//		Options options = new Options();
//		options.addOption(recursiveOption);
//		options.addOption(deleteOption);
//		options.addOption(progressOption);
//		return options;
//	}

	@Override
	public String getUsage() {
		return "[source URI] [target URI]";
	}

	public static void main(String[] args) {
		DescribedCommand.mainImpl(new FileSync(), args);
	}

	@Override
	public String getDescription() {
		return "Synchronises files";
	}

}
