package org.argeo.cms.jshell;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.StandardProtocolFamily;
import java.net.URI;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringJoiner;
import java.util.UUID;

import org.argeo.api.cms.CmsLog;
import org.argeo.internal.cms.jshell.osgi.OsgiExecutionControlProvider;

import jdk.jshell.tool.JavaShellToolBuilder;

class LocalJShellSession implements Runnable {
	private final static CmsLog log = CmsLog.getLog(LocalJShellSession.class);

	private UUID uuid;
	private Path sessionDir;

	private String fromBundle = "org.argeo.cms.jshell";

	private Path stdioPath;
	private Path stderrPath;
	private Path cmdioPath;

	private Thread replThread;

	LocalJShellSession(Path sessionDir) {
		this.sessionDir = sessionDir;
		this.uuid = UUID.fromString(sessionDir.getFileName().toString());

		stdioPath = sessionDir.resolve(JShellClient.STDIO);

		replThread = new Thread(this, "JShell " + sessionDir);
		replThread.start();
	}

	public void run() {
		log.debug(() -> "Started JShell session " + sessionDir);
		try (SocketPipeMirror std = new SocketPipeMirror()) {
			// prepare jshell tool builder
			JavaShellToolBuilder builder = JavaShellToolBuilder.builder();
			builder.in(std.getInputStream(), null);
			builder.interactiveTerminal(true);
			builder.out(new PrintStream(std.getOutputStream()));

			// UnixDomainSocketAddress ioSocketAddress = JSchellClient.ioSocketAddress();
			// Files.deleteIfExists(ioSocketAddress.getPath());
			UnixDomainSocketAddress stdSocketAddress = UnixDomainSocketAddress.of(stdioPath);

			try (ServerSocketChannel stdServerChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
				stdServerChannel.bind(stdSocketAddress);
				try (SocketChannel channel = stdServerChannel.accept()) {
					std.open(channel);

					String frameworkLocation = System.getProperty("osgi.framework");
					StringJoiner classpath = new StringJoiner(File.pathSeparator);
					classpath.add(Paths.get(URI.create(frameworkLocation)).toAbsolutePath().toString());

					ClassLoader cmsJShellBundleCL = OsgiExecutionControlProvider.class.getClassLoader();
					ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
					try {
						// we need our own class loader so that Java service loader
						// finds our ExecutionControlProvider implementation
						Thread.currentThread().setContextClassLoader(cmsJShellBundleCL);
						//
						// START JSHELL
						//
						int exitCode = builder.start("--execution", "osgi:bundle(" + fromBundle + ")", "--class-path",
								classpath.toString());
						//
						log.debug("JShell " + sessionDir + " completed with exit code " + exitCode);
					} finally {
						Thread.currentThread().setContextClassLoader(currentContextClassLoader);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("JShell " + sessionDir + " failed", e);
		} finally {
			cleanUp();
		}
	}

	void cleanUp() {
		try {
			if (Files.exists(stdioPath))
				Files.delete(stdioPath);
			if (Files.exists(sessionDir))
				Files.delete(sessionDir);
		} catch (IOException e) {
			log.error("Cannot clean up JShell " + sessionDir, e);
		}
	}

//		void addChild(Path p) throws IOException {
//			if (replThread != null)
//				throw new IllegalStateException("JShell " + sessionDir + " is already started");
//
//			if (STDIO.equals(p.getFileName().toString())) {
//				stdioPath = p;
//			} else if (STDERR.equals(p.getFileName().toString())) {
//				stderrPath = p;
//			} else if (CMDIO.equals(p.getFileName().toString())) {
//				cmdioPath = p;
//			} else {
//				log.warn("Unkown file name " + p.getFileName() + " in " + sessionDir);
//			}
//
//			// check that all paths are available
//			// if (stdioPath != null && stderrPath != null && cmdioPath != null) {
//			if (stdioPath != null) {
//				replThread = new Thread(this, "JShell " + sessionDir);
//				replThread.start();
//			}
//		}

}
