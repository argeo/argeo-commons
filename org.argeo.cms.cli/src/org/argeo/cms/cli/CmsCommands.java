package org.argeo.cms.cli;

import java.net.URI;
import java.util.List;

import org.apache.commons.cli.Option;
import org.argeo.api.cli.CLine;
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

//	@Override
//	public String getDescription() {
//		return "Utilities related to an Argeo CMS";
//	}

	class Ping extends DescribedCommand<Void> {
		@Override
		protected Class<? extends Enum<?>> getOptClass() {
			return CmsOpt.class;
		}

		@Override
		public Void execute(CLine cLine) {
			URI uri = cLine.get(CmsOpt.connect, URI.class).orElseThrow();
			if ("".equals(uri.getPath())) {
				uri = URI.create(uri.toString() + "/cms/status/ping");
			}
			new WebSocketPing(uri).run();
			return null;
		}

//		@Override
//		public String getUsage() {
//			return "[ws|wss]://host:port/";
//		}
//
//		@Override
//		public String getDescription() {
//			return "Test whether an Argeo CMS is available, without auhtentication";
//		}

	}

	class Get extends DescribedCommand<String> {

		@Override
		protected Class<? extends Enum<?>> getOptClass() {
			return CmsOpt.class;
		}

		@Override
		public String execute(CLine cLine) {
			List<String> remaining = cLine.getPlainArgs();
			String additionalUri = null;
			if (remaining.size() != 0) {
				additionalUri = remaining.get(0);
			}

			URI connectUri = cLine.get(CmsOpt.connect, URI.class).orElseThrow();
			CmsClient cmsClient = new CmsClient(connectUri);
			return additionalUri != null ? cmsClient.getAsString(URI.create(additionalUri)) : cmsClient.getAsString();
		}

//		@Override
//		public String getUsage() {
//			return "[URI]";
//		}
//
//		@Override
//		public String getDescription() {
//			return "Retrieve this URI as a string";
//		}

	}

	class Status extends DescribedCommand<String> {

		@Override
		protected Class<? extends Enum<?>> getOptClass() {
			return CmsOpt.class;
		}

		@Override
		public String execute(CLine cLine) {
			URI uri = cLine.get(CmsOpt.connect, URI.class).orElseThrow();
			CmsClient cmsClient = new CmsClient(uri);
			return cmsClient.getAsString(URI.create("/cms/status"));
		}

//		@Override
//		public String getUsage() {
//			return "[URI]";
//		}
//
//		@Override
//		public String getDescription() {
//			return "Retrieve the CMS status as a string";
//		}

	}
}
