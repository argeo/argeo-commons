package org.argeo.sync.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.SshAgentFactory;
import org.apache.sshd.agent.local.LocalAgentFactory;
import org.apache.sshd.agent.unix.UnixAgentFactory;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.config.keys.ClientIdentityLoader;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.subsystem.sftp.SftpFileSystem;
import org.apache.sshd.client.subsystem.sftp.SftpFileSystemProvider;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.apache.sshd.common.util.io.NoCloseOutputStream;

public class SshSync {
	private final static Log log = LogFactory.getLog(SshSync.class);

	public static void main(String[] args) {

		try (SshClient client = SshClient.setUpDefaultClient()) {
			client.start();
			boolean osAgent = false;
			SshAgentFactory agentFactory = osAgent ? new UnixAgentFactory() : new LocalAgentFactory();
			// SshAgentFactory agentFactory = new LocalAgentFactory();
			client.setAgentFactory(agentFactory);
			SshAgent sshAgent = agentFactory.createClient(client);

			String login = System.getProperty("user.name");
			String host = "localhost";
			int port = 22;

			if (!osAgent) {
				String keyPath = "/home/" + login + "/.ssh/id_rsa";
				System.out.print(keyPath + ": ");
				Scanner s = new Scanner(System.in);
				String password = s.next();
				KeyPair keyPair = ClientIdentityLoader.DEFAULT.loadClientIdentity(keyPath,
						FilePasswordProvider.of(password));
				sshAgent.addIdentity(keyPair, "NO COMMENT");
			}

			List<? extends Map.Entry<PublicKey, String>> identities = sshAgent.getIdentities();
			for (Map.Entry<PublicKey, String> entry : identities) {
				System.out.println(entry.getValue() + " : " + entry.getKey());
			}

			ConnectFuture connectFuture = client.connect(login, host, port);
			connectFuture.await();
			ClientSession session = connectFuture.getSession();

			try {

//				session.addPasswordIdentity(new String(password));
				session.auth().verify(1000l);

				SftpFileSystemProvider fsProvider = new SftpFileSystemProvider(client);

				SftpFileSystem fs = fsProvider.newFileSystem(session);
				Path testPath = fs.getPath("/home/" + login + "/tmp");
				Files.list(testPath).forEach(System.out::println);
				test(testPath);

			} finally {
				client.stop();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static void test(Path testBase) {
		try {
			Path testPath = testBase.resolve("ssh-test.txt");
			Files.createFile(testPath);
			log.debug("Created file " + testPath);
			Files.delete(testPath);
			log.debug("Deleted " + testPath);
			String txt = "TEST\nTEST2\n";
			byte[] arr = txt.getBytes();
			Files.write(testPath, arr);
			log.debug("Wrote " + testPath);
			byte[] read = Files.readAllBytes(testPath);
			log.debug("Read " + testPath);
			Path testDir = testBase.resolve("testDir");
			log.debug("Resolved " + testDir);
			// Copy
			Files.createDirectory(testDir);
			log.debug("Created directory " + testDir);
			Path subsubdir = Files.createDirectories(testDir.resolve("subdir/subsubdir"));
			log.debug("Created sub directories " + subsubdir);
			Path copiedFile = testDir.resolve("copiedFile.txt");
			log.debug("Resolved " + copiedFile);
			Path relativeCopiedFile = testDir.relativize(copiedFile);
			log.debug("Relative copied file " + relativeCopiedFile);
			try (OutputStream out = Files.newOutputStream(copiedFile);
					InputStream in = Files.newInputStream(testPath)) {
				IOUtils.copy(in, out);
			}
			log.debug("Copied " + testPath + " to " + copiedFile);
			Files.delete(testPath);
			log.debug("Deleted " + testPath);
			byte[] copiedRead = Files.readAllBytes(copiedFile);
			log.debug("Read " + copiedFile);
			// Browse directories
			DirectoryStream<Path> files = Files.newDirectoryStream(testDir);
			int fileCount = 0;
			Path listedFile = null;
			for (Path file : files) {
				fileCount++;
				if (!Files.isDirectory(file))
					listedFile = file;
			}
			log.debug("Listed " + testDir);
			// Generic attributes
			Map<String, Object> attrs = Files.readAttributes(copiedFile, "*");
			log.debug("Read attributes of " + copiedFile + ": " + attrs.keySet());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
}
