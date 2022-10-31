package org.argeo.cms.cli;

import java.net.URI;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.argeo.api.cli.CommandArgsException;
import org.argeo.api.cli.CommandsCli;
import org.argeo.api.cli.DescribedCommand;
import org.argeo.cms.client.WsPing;

public class CmsCommands extends CommandsCli {

	public CmsCommands(String commandName) {
		super(commandName);
		addCommand("ping", new Ping());
	}

	@Override
	public String getDescription() {
		return "Utilities related to an Argeo CMS";
	}

	class Ping implements DescribedCommand<Void> {

		@Override
		public Void apply(List<String> t) {
			CommandLine line = toCommandLine(t);
			List<String> remaining = line.getArgList();
			if (remaining.size() == 0) {
				throw new CommandArgsException("There must be at least one argument");
			}
			String uriArg = remaining.get(0);
			// TODO make it more robust (trailing /, etc.)
			URI uri = URI.create(uriArg);
			if ("".equals(uri.getPath())) {
				uri = URI.create(uri.toString() + "/cms/status/ping");
			}
			new WsPing(uri).run();
			return null;
		}

		@Override
		public String getUsage() {
			return "[ws|wss]://host:port/";
		}

		@Override
		public String getDescription() {
			return "Test whether an Argeo CMS is available, without auhtentication";
		}

	}
}
