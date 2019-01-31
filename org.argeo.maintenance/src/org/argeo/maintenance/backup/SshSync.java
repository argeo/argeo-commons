package org.argeo.maintenance.backup;

import java.io.Console;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.apache.sshd.common.util.io.NoCloseOutputStream;

public class SshSync {
	public static void main(String[] args) {
		
		String login = System.getProperty("user.name");
		Console console = System.console();
		char[] password = console.readPassword();
		String host = "localhost";
		int port = 22;

		try (SshClient client = SshClient.setUpDefaultClient()) {
			client.start();

//			SimpleClient simpleClient= AbstractSimpleClientSessionCreator.wrap(client, null);
//			simpleClient.sessionLogin(host, login, password);

			ConnectFuture connectFuture = client.connect(login, host, port);
			connectFuture.await();
			ClientSession session = connectFuture.getSession();

			try {

				session.addPasswordIdentity(new String(password));
				session.auth().verify(1000l);

				try (ClientChannel channel = session.createChannel(ClientChannel.CHANNEL_SHELL)) {
					channel.setIn(new NoCloseInputStream(System.in));
					channel.setOut(new NoCloseOutputStream(System.out));
					channel.setErr(new NoCloseOutputStream(System.err));
					channel.open();

					Set<ClientChannelEvent> events = new HashSet<>();
					events.add(ClientChannelEvent.CLOSED);
					channel.waitFor(events, 0);
				} finally {
					session.close(false);
				}
			} finally {
				client.stop();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
