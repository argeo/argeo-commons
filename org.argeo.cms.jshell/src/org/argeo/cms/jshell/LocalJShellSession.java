package org.argeo.cms.jshell;

import static java.net.StandardProtocolFamily.UNIX;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executors;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.util.CurrentSubject;
import org.argeo.cms.util.FsUtils;
import org.argeo.internal.cms.jshell.osgi.OsgiExecutionControlProvider;

import jdk.jshell.tool.JavaShellToolBuilder;

/** A JShell session based on local UNIX sockets. */
class LocalJShellSession implements Runnable {
	private final static CmsLog log = CmsLog.getLog(LocalJShellSession.class);

	private UUID uuid;
	private Path sessionDir;
	private Path socketsDir;

	private Path stdPath;
	private Path ctlPath;

	private Thread replThread;

	private LoginContext loginContext;

	private Long bundleId;

	private final boolean interactive;

	LocalJShellSession(Path sessionDir, Path bundleIdDir, boolean interactive) {
		this.interactive = interactive;
		try {
			this.sessionDir = sessionDir;
			this.uuid = UUID.fromString(sessionDir.getFileName().toString());
			bundleId = Long.parseLong(bundleIdDir.getFileName().toString());
			socketsDir = bundleIdDir.resolve(uuid.toString());
			Files.createDirectories(socketsDir);

			stdPath = socketsDir.resolve(JShellClient.STD);
			Files.createSymbolicLink(sessionDir.resolve(JShellClient.STD), stdPath);

			ctlPath = socketsDir.resolve(JShellClient.CTL);
			Files.createSymbolicLink(sessionDir.resolve(JShellClient.CTL), ctlPath);

			// TODO proper login
			try {
				loginContext = new LoginContext(CmsAuth.DATA_ADMIN.getLoginContextName());
				loginContext.login();
			} catch (LoginException e1) {
				throw new RuntimeException("Could not login as data admin", e1);
			} finally {
			}

		} catch (IOException e) {
			log.error("Cannot initiate local session " + uuid, e);
			cleanUp();
			return;
		}
		replThread = new Thread(() -> CurrentSubject.callAs(loginContext.getSubject(), Executors.callable(this)),
				"JShell " + sessionDir);
		replThread.start();
	}

	public void run() {

		log.debug(() -> "Started JShell session " + sessionDir);
		try (SocketPipeMirror std = new SocketPipeMirror(JShellClient.STD + " " + uuid); //
				SocketPipeMirror ctl = new SocketPipeMirror(JShellClient.CTL + " " + uuid);) {
			// prepare jshell tool builder
			String feedbackMode;
			JavaShellToolBuilder builder = JavaShellToolBuilder.builder();
			if (interactive) {
				builder.in(std.getInputStream(), null);
				builder.out(new PrintStream(std.getOutputStream()));
				builder.err(new PrintStream(ctl.getOutputStream()));
				builder.interactiveTerminal(true);
				feedbackMode = "concise";
			} else {
				builder.in(ctl.getInputStream(), std.getInputStream());
				PrintStream cmdOut = new PrintStream(ctl.getOutputStream());
				PrintStream discard = new PrintStream(OutputStream.nullOutputStream());
				builder.out(cmdOut, discard, new PrintStream(std.getOutputStream()));
				builder.err(cmdOut);
				builder.promptCapture(true);
				feedbackMode = "silent";
			}

			UnixDomainSocketAddress stdSocketAddress = UnixDomainSocketAddress.of(stdPath);
			UnixDomainSocketAddress ctlSocketAddress = UnixDomainSocketAddress.of(ctlPath);

			try (ServerSocketChannel stdServerChannel = ServerSocketChannel.open(UNIX);
					ServerSocketChannel ctlServerChannel = ServerSocketChannel.open(UNIX);) {
				stdServerChannel.bind(stdSocketAddress);
				ctlServerChannel.bind(ctlSocketAddress);
				try (SocketChannel stdChannel = stdServerChannel.accept();
						SocketChannel ctlChannel = ctlServerChannel.accept();) {
					std.open(stdChannel);
					ctl.open(ctlChannel);

					ClassLoader cmsJShellBundleCL = OsgiExecutionControlProvider.class.getClassLoader();
					ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
					try {
						String classpath = OsgiExecutionControlProvider.getBundleClasspath(bundleId);
						Path bundleStartupScript = OsgiExecutionControlProvider.getBundleStartupScript(bundleId);
						// we need our own class loader so that Java service loader
						// finds our ExecutionControlProvider implementation
						Thread.currentThread().setContextClassLoader(cmsJShellBundleCL);
						//
						// START JSHELL
						//
						int exitCode = builder.start("--execution", "osgi:bundle(" + bundleId + ")", "--class-path",
								classpath, "--startup", bundleStartupScript.toString(), "--feedback", feedbackMode);
						//
						log.debug("JShell " + sessionDir + " completed with exit code " + exitCode);
					} finally {
						Thread.currentThread().setContextClassLoader(currentContextClassLoader);
					}
				} finally {
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("JShell " + sessionDir + " failed", e);
		} finally {
			cleanUp();
		}
	}

	private void cleanUp() {
		try {
			if (Files.exists(socketsDir))
				FsUtils.delete(socketsDir);
			if (Files.exists(sessionDir))
				FsUtils.delete(sessionDir);
		} catch (IOException e) {
			log.error("Cannot clean up JShell " + sessionDir, e);
		}

		if (loginContext != null)
			try {
				loginContext.logout();
			} catch (LoginException e) {
				log.error("Cannot log out JShell " + sessionDir, e);
			}
	}
}
