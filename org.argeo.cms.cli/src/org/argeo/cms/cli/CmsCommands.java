package org.argeo.cms.cli;

import java.net.URI;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.argeo.api.cli.CommandsCli;
import org.argeo.api.cli.DescribedCommand;
import org.argeo.cms.client.CmsClient;
import org.argeo.cms.client.WebSocketPing;

/** Commands dealing with CMS. */
public class CmsCommands extends CommandsCli {
	final static Option connectOption = Option.builder().option("c").longOpt("connect").desc("server to connect to")
			.hasArg(true).build();

	public CmsCommands(String commandName) {
		super(commandName);
		addCommand("ping", new Ping());
		addCommand("get", new Get());
		addCommand("status", new Status());
		addCommand("event", new EventCommands("event"));
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

	class Get implements DescribedCommand<String> {

		@Override
		public Options getOptions() {
			Options options = new Options();
			options.addOption(connectOption);
			return options;
		}

		@Override
		public String apply(List<String> t) {
			CommandLine line = toCommandLine(t);
			List<String> remaining = line.getArgList();
			String additionalUri = null;
			if (remaining.size() != 0) {
				additionalUri = remaining.get(0);
			}

			String connectUri = line.getOptionValue(connectOption);
			CmsClient cmsClient = new CmsClient(URI.create(connectUri));
			return additionalUri != null ? cmsClient.getAsString(URI.create(additionalUri)) : cmsClient.getAsString();
		}

		@Override
		public String getUsage() {
			return "[URI]";
		}

		@Override
		public String getDescription() {
			return "Retrieve this URI as a string";
		}

	}

	class Status implements DescribedCommand<String> {

		@Override
		public Options getOptions() {
			Options options = new Options();
			options.addOption(connectOption);
			return options;
		}

		@Override
		public String apply(List<String> t) {
			CommandLine line = toCommandLine(t);
			String connectUri = line.getOptionValue(connectOption);
			CmsClient cmsClient = new CmsClient(URI.create(connectUri));
			return cmsClient.getAsString(URI.create("/cms/status"));
		}

		@Override
		public String getUsage() {
			return "[URI]";
		}

		@Override
		public String getDescription() {
			return "Retrieve the CMS status as a string";
		}

	}
}
