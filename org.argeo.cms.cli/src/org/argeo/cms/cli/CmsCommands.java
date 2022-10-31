package org.argeo.cms.cli;

import java.net.URI;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.argeo.api.cli.CommandArgsException;
import org.argeo.api.cli.CommandsCli;
import org.argeo.api.cli.DescribedCommand;
import org.argeo.cms.client.WebSocketEventClient;
import org.argeo.cms.client.WebSocketPing;

public class CmsCommands extends CommandsCli {
	final static Option connectOption = Option.builder().option("c").longOpt("connect").desc("server to connect to")
			.hasArg(true).build();

	public CmsCommands(String commandName) {
		super(commandName);
		addCommand("ping", new Ping());
		addCommand("event", new Events());
	}

	@Override
	public String getDescription() {
		return "Utilities related to an Argeo CMS";
	}

	class Ping implements DescribedCommand<Void> {
		@Override
		public Options getOptions() {
			Options options = new Options();
			options.addOption(connectOption);
			return options;
		}

		@Override
		public Void apply(List<String> t) {
			CommandLine line = toCommandLine(t);
			String uriArg = line.getOptionValue(connectOption);
			// TODO make it more robust (trailing /, etc.)
			URI uri = URI.create(uriArg);
			if ("".equals(uri.getPath())) {
				uri = URI.create(uri.toString() + "/cms/status/ping");
			}
			new WebSocketPing(uri).run();
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

	class Events implements DescribedCommand<Void> {

		@Override
		public Options getOptions() {
			Options options = new Options();
			options.addOption(connectOption);
			return options;
		}

		@Override
		public Void apply(List<String> t) {
			CommandLine line = toCommandLine(t);
			List<String> remaining = line.getArgList();
			if (remaining.size() == 0) {
				throw new CommandArgsException("There must be at least one argument");
			}
			String topic = remaining.get(0);

			String uriArg = line.getOptionValue(connectOption);
			// TODO make it more robust (trailing /, etc.)
			URI uri = URI.create(uriArg);
			if ("".equals(uri.getPath())) {
				uri = URI.create(uri.toString() + "/cms/status/event/" + topic);
			}
			new WebSocketEventClient(uri).run();
			return null;
		}

		@Override
		public String getUsage() {
			return "TOPIC";
		}

		@Override
		public String getDescription() {
			return "Listen to events on a topic";
		}

	}
}
