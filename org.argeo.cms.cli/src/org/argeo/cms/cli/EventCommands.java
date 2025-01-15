package org.argeo.cms.cli;

import java.net.URI;
import java.util.List;

import org.argeo.api.cli.CLine;
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

//	@Override
//	public String getDescription() {
//		return "Utilities related to an Argeo CMS";
//	}

	class EventListent extends DescribedCommand<Void> {

//		@Override
//		public Options getOptions() {
//			Options options = new Options();
//			options.addOption(CmsCommands.connectOption);
//			return options;
//		}

		@Override
		public Void execute(CLine cLine) {
			URI uri = cLine.get(CmsOpt.connect, URI.class).orElseThrow();
			List<String> remaining = cLine.getPlainArgs();
			if (remaining.size() == 0) {
				throw new CommandArgsException("There must be at least one argument");
			}
			String topic = remaining.get(0);
			if ("".equals(uri.getPath())) {
				uri = URI.create(uri.toString() + "/cms/status/event/" + topic);
			}
			new WebSocketEventClient(uri).run();
			return null;
		}

		@Override
		protected Class<? extends Enum<?>> getOptClass() {
			return CmsOpt.class;
		}

//		@Override
//		public String getUsage() {
//			return "TOPIC";
//		}
//
//		@Override
//		public String getDescription() {
//			return "Listen to events on a topic";
//		}

	}
}
