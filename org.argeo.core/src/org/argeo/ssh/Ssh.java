package org.argeo.ssh;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.apache.sshd.common.util.io.NoCloseOutputStream;

public class Ssh extends AbstractSsh {
	private final URI uri;

	public Ssh(URI uri) {
		this.uri = uri;
		openSession(uri);
	}

	static void openShell(ClientSession session) {
		try (ClientChannel channel = session.createChannel(ClientChannel.CHANNEL_SHELL)) {
			channel.setIn(new NoCloseInputStream(System.in));
			channel.setOut(new NoCloseOutputStream(System.out));
			channel.setErr(new NoCloseOutputStream(System.err));
			channel.open();

			Set<ClientChannelEvent> events = new HashSet<>();
			events.add(ClientChannelEvent.CLOSED);
			channel.waitFor(events, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			session.close(false);
		}
	}

	public static void main(String[] args) {
		Options options = getOptions();
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(options, args);
			List<String> remaining = line.getArgList();
			if (remaining.size() == 0) {
				System.err.println("There must be at least one argument");
				printHelp(options);
				System.exit(1);
			}
			URI uri = new URI("ssh://" + remaining.get(0));
			List<String> command = new ArrayList<>();
			if (remaining.size() > 1) {
				for (int i = 1; i < remaining.size(); i++) {
					command.add(remaining.get(i));
				}
			}

			// auth
			Ssh ssh = new Ssh(uri);
			ssh.authenticate();

			if (command.size() == 0) {// shell
				openShell(ssh.getSession());
			} else {// execute command

			}
			ssh.closeSession();
		} catch (Exception exp) {
			exp.printStackTrace();
			printHelp(options);
			System.exit(1);
		} finally {

		}
	}

	public static Options getOptions() {
		Options options = new Options();
//		options.addOption("p", true, "port");
		options.addOption(Option.builder("p").hasArg().argName("port").desc("port of the SSH server").build());

		return options;
	}

	public static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("ssh [username@]hostname", options, true);
	}
}
