package org.argeo.cms.ssh;

import java.io.Console;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.io.input.NoCloseInputStream;
import org.apache.sshd.common.util.io.output.NoCloseOutputStream;
import org.apache.sshd.sftp.client.fs.SftpFileSystemProvider;
import org.argeo.api.cms.CmsLog;

public abstract class AbstractSsh {
	private final static CmsLog log = CmsLog.getLog(AbstractSsh.class);

	private SshClient sshClient;
	private SftpFileSystemProvider sftpFileSystemProvider;

	private boolean passwordSet = false;
	private ClientSession session;

	private SshKeyPair sshKeyPair;

	public synchronized SshClient getSshClient() {
		if (sshClient == null) {
			long begin = System.currentTimeMillis();
			sshClient = SshClient.setUpDefaultClient();
			sshClient.start();
			long duration = System.currentTimeMillis() - begin;
			if (log.isDebugEnabled())
				log.debug("SSH client started in " + duration + " ms");
			Runtime.getRuntime().addShutdownHook(new Thread(() -> sshClient.stop(), "Stop SSH client"));
		}
		return sshClient;
	}

	synchronized SftpFileSystemProvider getSftpFileSystemProvider() {
		if (sftpFileSystemProvider == null) {
			sftpFileSystemProvider = new SftpFileSystemProvider(sshClient);
		}
		return sftpFileSystemProvider;
	}

	public void authenticate() {
		if (sshKeyPair != null) {
			session.addPublicKeyIdentity(sshKeyPair.asKeyPair());
		} else {

			if (!passwordSet) {
				String password;
				Console console = System.console();
				if (console == null) {// IDE
					System.out.print("Password: ");
					try (Scanner s = new Scanner(System.in)) {
						password = s.next();
					}
				} else {
					console.printf("Password: ");
					char[] pwd = console.readPassword();
					password = new String(pwd);
					Arrays.fill(pwd, ' ');
				}
				session.addPasswordIdentity(password);
				passwordSet = true;
			}
		}
		verifyAuth();
	}

	public void verifyAuth() {
		try {
			session.auth().verify(1000l);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot verify auth", e);
		}
	}

	public static char[] readPassword() {
		Console console = System.console();
		if (console == null) {// IDE
			System.out.print("Password: ");
			try (Scanner s = new Scanner(System.in)) {
				String password = s.next();
				return password.toCharArray();
			}
		} else {
			console.printf("Password: ");
			char[] pwd = console.readPassword();
			return pwd;
		}
	}

	void addPassword(String password) {
		session.addPasswordIdentity(password);
	}

	void loadKey(String password) {
		loadKey(password, System.getProperty("user.home") + "/.ssh/id_rsa");
	}

	void loadKey(String password, String keyPath) {
//		try {
//			KeyPair keyPair = ClientIdentityLoader.DEFAULT.loadClientIdentity(keyPath,
//					FilePasswordProvider.of(password));
//			session.addPublicKeyIdentity(keyPair);
//		} catch (IOException | GeneralSecurityException e) {
//			throw new IllegalStateException(e);
//		}
	}

	void openSession(URI uri) {
		openSession(uri.getUserInfo(), uri.getHost(), uri.getPort() > 0 ? uri.getPort() : null);
	}

	void openSession(String login, String host, Integer port) {
		if (session != null)
			throw new IllegalStateException("Session is already open");

		if (host == null)
			host = "localhost";
		if (port == null)
			port = 22;
		if (login == null)
			login = System.getProperty("user.name");
		String password = null;
		int sepIndex = login.indexOf(':');
		if (sepIndex > 0)
			if (sepIndex + 1 < login.length()) {
				password = login.substring(sepIndex + 1);
				login = login.substring(0, sepIndex);
			} else {
				throw new IllegalArgumentException("Illegal authority: " + login);
			}
		try {
			ConnectFuture connectFuture = getSshClient().connect(login, host, port);
			connectFuture.await();
			ClientSession session = connectFuture.getSession();
			if (password != null) {
				session.addPasswordIdentity(password);
				passwordSet = true;
			}
			this.session = session;
		} catch (IOException e) {
			throw new IllegalStateException("Cannot connect to " + host + ":" + port);
		}
	}

	public void closeSession() {
		if (session == null)
			throw new IllegalStateException("No session is open");
		try {
			session.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			session = null;
		}
	}

	ClientSession getSession() {
		return session;
	}

	public void setSshKeyPair(SshKeyPair sshKeyPair) {
		this.sshKeyPair = sshKeyPair;
	}

	public static void openShell(AbstractSsh ssh) {
		openShell(ssh.getSession());
	}

	public static void openShell(ClientSession session) {
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

	static URI toUri(String username, String host, int port) {
		try {
			if (username == null)
				username = "root";
			return new URI("ssh://" + username + "@" + host + ":" + port);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Cannot generate SSH URI to " + host + ":" + port + " for " + username,
					e);
		}
	}

}
