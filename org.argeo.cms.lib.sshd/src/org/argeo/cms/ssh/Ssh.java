package org.argeo.cms.ssh;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/** Create an SSH shell. */
public class Ssh extends AbstractSsh {
	private final URI uri;

	public Ssh(String username, String host, int port) {
		this(AbstractSsh.toUri(username, host, port));
	}

	public Ssh(URI uri) {
		this.uri = uri;
		openSession(uri);
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
				AbstractSsh.openShell(ssh.getSession());
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

	public URI getUri() {
		return uri;
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
