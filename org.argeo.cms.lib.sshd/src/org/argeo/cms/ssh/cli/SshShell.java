package org.argeo.cms.ssh.cli;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.SshAgentFactory;
import org.apache.sshd.agent.local.LocalAgentFactory;
import org.apache.sshd.agent.unix.UnixAgentFactory;
import org.apache.sshd.client.config.keys.ClientIdentityLoader;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.argeo.api.cli.CommandArgsException;
import org.argeo.api.cli.DescribedCommand;
import org.argeo.cms.ssh.AbstractSsh;
import org.argeo.cms.ssh.Ssh;

public class SshShell implements DescribedCommand<String> {
	private Option portOption;

	@Override
	public Options getOptions() {
		Options options = new Options();
		portOption = Option.builder().option("p").longOpt("port").hasArg().desc("port to connect to").build();
		options.addOption(portOption);
		return options;
	}

	@Override
	public String apply(List<String> args) {
		CommandLine cl = toCommandLine(args);
		String portStr = cl.getOptionValue(portOption);
		if (portStr == null)
			portStr = "22";

		if (cl.getArgList().size() == 0)
			throw new CommandArgsException("Host must be provided");
		String host = cl.getArgList().get(0);

		String uriStr = "ssh://" + host + ":" + portStr + "/";
		// System.out.println(uriStr);
		URI uri = URI.create(uriStr);

		Ssh ssh = null;
		try {
			ssh = new Ssh(uri);
			boolean osAgent;
			SshAgent sshAgent;
			try {
				String sshAuthSockentEnv = System.getenv(SshAgent.SSH_AUTHSOCKET_ENV_NAME);
				if (sshAuthSockentEnv != null) {
					ssh.getSshClient().getProperties().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, sshAuthSockentEnv);
					SshAgentFactory agentFactory = new UnixAgentFactory();
					ssh.getSshClient().setAgentFactory(agentFactory);
					sshAgent = agentFactory.createClient(null, ssh.getSshClient());
					osAgent = true;
				} else {
					osAgent = false;
				}
			} catch (Exception e) {
				e.printStackTrace();
				osAgent = false;
			}

			if (!osAgent) {
				SshAgentFactory agentFactory = new LocalAgentFactory();
				ssh.getSshClient().setAgentFactory(agentFactory);
				sshAgent = agentFactory.createClient(null, ssh.getSshClient());
				String keyPath = System.getProperty("user.home") + "/.ssh/id_rsa";

				char[] keyPassword = AbstractSsh.readPassword();
				NamedResource namedResource = new NamedResource() {

					@Override
					public String getName() {
						return keyPath;
					}
				};
				KeyPair keyPair = ClientIdentityLoader.DEFAULT
						.loadClientIdentities(null, namedResource, FilePasswordProvider.of(new String(keyPassword)))
						.iterator().next();
				sshAgent.addIdentity(keyPair, "NO COMMENT");
			}

//				char[] keyPassword = AbstractSsh.readPassword();
//				SshKeyPair keyPair = SshKeyPair.loadDefault(keyPassword);
//				Arrays.fill(keyPassword, '*');
//				ssh.setSshKeyPair(keyPair);
//				ssh.authenticate();
			ssh.verifyAuth();

			long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
			System.out.println("Ssh available in " + jvmUptime + " ms.");

			AbstractSsh.openShell(ssh);
		} catch (IOException | GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (ssh != null)
				ssh.closeSession();
		}
		return null;
	}

	@Override
	public String getUsage() {
		return "<hostname>";
	}

	@Override
	public String getDescription() {
		return "Opens a remote shell";
	}

}