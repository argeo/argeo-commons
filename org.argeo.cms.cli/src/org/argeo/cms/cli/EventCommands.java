package org.argeo.cms.cli;

import java.net.URI;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.argeo.api.cli.CommandArgsException;
import org.argeo.api.cli.CommandsCli;
import org.argeo.api.cli.DescribedCommand;
import org.argeo.cms.client.WebSocketEventClient;

/** Commands dealing with CMS events. */
public class EventCommands extends CommandsCli {
	public EventCommands(String commandName) {
		super(commandName);
		addCommand("listen", new EventListent());
	}

	@Override
	public String getDescription() {
		return "Utilities related to an Argeo CMS";
	}

	class EventListent implements DescribedCommand<Void> {

		@Override
		public Options getOptions() {
			Options options = new Options();
			options.addOption(CmsCommands.connectOption);
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

			String uriArg = line.getOptionValue(CmsCommands.connectOption);
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
