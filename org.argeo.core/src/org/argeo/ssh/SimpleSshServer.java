package org.argeo.ssh;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.argeo.util.os.OS;

/** A simple SSH server with some defaults. Supports SCP. */
@SuppressWarnings("restriction")
public class SimpleSshServer {
	private Integer port;
	private Path hostKeyPath;

	private SshServer sshd = null;

	public SimpleSshServer(Integer port, Path hostKeyPath) {
		this.port = port;
		this.hostKeyPath = hostKeyPath;
	}

	public void init() {
		try {
			sshd = SshServer.setUpDefaultServer();
			sshd.setPort(port);
			if (hostKeyPath == null)
				throw new IllegalStateException("An SSH server key must be set");
			sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyPath));
			// sshd.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i",
			// "-l" }));
			String[] shellCommand = OS.LOCAL.getDefaultShellCommand();
			sshd.setShellFactory(new ProcessShellFactory(shellCommand));
			sshd.setCommandFactory(new ScpCommandFactory());
			sshd.start();
		} catch (Exception e) {
			throw new RuntimeException("Cannot start SSH server on port " + port, e);
		}
	}

	public void destroy() {
		try {
			sshd.stop();
		} catch (IOException e) {
			throw new RuntimeException("Cannot stop SSH server on port " + port, e);
		}
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Path getHostKeyPath() {
		return hostKeyPath;
	}

	public void setHostKeyPath(Path hostKeyPath) {
		this.hostKeyPath = hostKeyPath;
	}

	public static void main(String[] args) {
		int port = 2222;
		Path hostKeyPath = Paths.get("hostkey.ser");
		try {
			if (args.length > 0)
				port = Integer.parseInt(args[0]);
			if (args.length > 1)
				hostKeyPath = Paths.get(args[1]);
		} catch (Exception e1) {
			printUsage();
		}

		SimpleSshServer sshServer = new SimpleSshServer(port, hostKeyPath);
		sshServer.init();
		Runtime.getRuntime().addShutdownHook(new Thread("Shutdown SSH server") {

			@Override
			public void run() {
				sshServer.destroy();
			}
		});
		try {
			synchronized (sshServer) {
				sshServer.wait();
			}
		} catch (InterruptedException e) {
			sshServer.destroy();
		}

	}

	public static void printUsage() {
		System.out.println("java " + SimpleSshServer.class.getName() + " [port] [server key path]");
	}

}
