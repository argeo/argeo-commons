package org.argeo.ssh;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.keys.ClientIdentityLoader;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.subsystem.sftp.SftpFileSystemProvider;
import org.apache.sshd.common.config.keys.FilePasswordProvider;

abstract class AbstractSsh {
	private final static Log log = LogFactory.getLog(AbstractSsh.class);

	private static SshClient sshClient;
	private static SftpFileSystemProvider sftpFileSystemProvider;

	private boolean passwordSet = false;
	private ClientSession session;

	synchronized SshClient getSshClient() {
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
		if(sftpFileSystemProvider==null) {
			sftpFileSystemProvider = new SftpFileSystemProvider(sshClient);
		}
		return sftpFileSystemProvider;
	}

	void authenticate() {
		try {
			session.auth().verify(1000l);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	void addPassword(String password) {
		session.addPasswordIdentity(password);
	}

	void loadKey(String password) {
		loadKey(password, System.getProperty("user.home") + "/.ssh/id_rsa");
	}

	void loadKey(String password, String keyPath) {
		try {
			KeyPair keyPair = ClientIdentityLoader.DEFAULT.loadClientIdentity(keyPath,
					FilePasswordProvider.of(password));
			session.addPublicKeyIdentity(keyPair);
		} catch (IOException | GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
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

	void closeSession() {
		if (session != null)
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

}
