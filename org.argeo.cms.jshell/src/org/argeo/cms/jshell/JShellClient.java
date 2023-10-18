package org.argeo.cms.jshell;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.TRACE;
import static java.net.StandardProtocolFamily.UNIX;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.System.Logger;
import java.lang.management.ManagementFactory;
import java.net.StandardSocketOptions;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A JShell client to a local CMS node. */
public class JShellClient {
	private final static Logger logger = System.getLogger(JShellClient.class.getName());

	public final static String STD = "std";
	public final static String CTL = "ctl";

	public final static String JSH = "jsh";
	public final static String JTERM = "jterm";

	private static String sttyExec = "/usr/bin/stty";

	/** Benchmark based on uptime. */
	private static boolean benchmark = false;

	/**
	 * The real path (following symbolic links) to the directory were to create
	 * sessions.
	 */
	private Path localBase;

	/** The symbolic name of the bundle from which to run. */
	private String symbolicName;

	/** The script to run. */
	private Path script;
	/** Additional arguments of the script */
	private List<String> scriptArgs;

	private String ttyConfig;
	private boolean terminal;

	/** Workaround to be able to test in Eclipse console */
	private boolean inEclipse = false;

	public JShellClient(Path targetStateDirectory, String symbolicName, Path script, List<String> scriptArgs) {
		try {
			this.terminal = System.console() != null && script == null;
			if (inEclipse && script == null)
				terminal = true;
			if (terminal) {
				localBase = targetStateDirectory.resolve(JTERM);
			} else {
				localBase = targetStateDirectory.resolve(JSH);
			}
			if (Files.isSymbolicLink(localBase)) {
				localBase = localBase.toRealPath();
			}
			this.symbolicName = symbolicName;
			this.script = script;
			this.scriptArgs = scriptArgs == null ? new ArrayList<>() : scriptArgs;
		} catch (IOException e) {
			throw new IllegalStateException("Cannot initialise client", e);
		}
	}

	public void run() {
		try {
			if (terminal)
				toRawTerminal();
			SocketPipeSource std = new SocketPipeSource(STD, script != null);
			std.setInputStream(System.in);
			std.setOutputStream(System.out);

			SocketPipeSource ctl = new SocketPipeSource(CTL, false);
			ctl.setOutputStream(System.err);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//				System.out.println("\nShutting down...");
				toOriginalTerminal();
				std.shutdown();
				ctl.shutdown();
			}, "Shut down JShell client"));

			Path bundleSnDir = localBase.resolve(symbolicName);
			if (!Files.exists(bundleSnDir))
				Files.createDirectory(bundleSnDir);
			UUID uuid = UUID.randomUUID();
			Path sessionDir = bundleSnDir.resolve(uuid.toString());

			// creating the directory will trigger opening of the session on server side
			Files.createDirectory(sessionDir);

			Path stdPath = sessionDir.resolve(JShellClient.STD);
			Path ctlPath = sessionDir.resolve(JShellClient.CTL);

			while (!(Files.exists(stdPath) && Files.exists(ctlPath))) {
				// TODO timeout
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// silent
				}
			}

			UnixDomainSocketAddress stdSocketAddress = UnixDomainSocketAddress.of(stdPath.toRealPath());
			UnixDomainSocketAddress ctlSocketAddress = UnixDomainSocketAddress.of(ctlPath.toRealPath());

			try (SocketChannel stdChannel = SocketChannel.open(UNIX);
					SocketChannel ctlChannel = SocketChannel.open(UNIX);) {
				ctlChannel.connect(ctlSocketAddress);
				ctl.process(ctlChannel);
				if (script != null) {
					new ScriptThread(ctlChannel).start();
				}
				stdChannel.connect(stdSocketAddress);
				std.process(stdChannel);

				while (!std.isCompleted() && !ctl.isCompleted()) {
					// isCompleted() will block
				}
			}
			if (benchmark)
				System.err.println(ManagementFactory.getRuntimeMXBean().getUptime());
			std.shutdown();
			ctl.shutdown();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			toOriginalTerminal();
		}

	}

	public static void main(String[] args) {
		try {
			if (benchmark)
				System.err.println(ManagementFactory.getRuntimeMXBean().getUptime());
			List<String> plainArgs = new ArrayList<>();
			Map<String, List<String>> options = new HashMap<>();
			String currentOption = null;
			for (int i = 0; i < args.length; i++) {
				if (args[i].startsWith("-")) {
					currentOption = args[i];
					if ("-h".equals(currentOption) || "--help".equals(currentOption)) {
						printHelp(System.out);
						return;
					}
					if (!options.containsKey(currentOption))
						options.put(currentOption, new ArrayList<>());
					i++;
					options.get(currentOption).add(args[i]);
				} else {
					plainArgs.add(args[i]);
				}
			}

			List<String> dir = opt(options, "-d", "--sockets-dir");
			if (dir.size() > 1)
				throw new IllegalArgumentException("Only one run directory can be specified");
			Path targetStateDirectory;
			if (dir.isEmpty())
				targetStateDirectory = Paths.get(System.getProperty("user.dir"));
			else {
				targetStateDirectory = Paths.get(dir.get(0));
				if (!Files.exists(targetStateDirectory)) {
					// we assume argument is the application id
					targetStateDirectory = getRunDir().resolve(dir.get(0));
				}
			}

			List<String> bundle = opt(options, "-b", "--bundle");
			if (bundle.size() > 1)
				throw new IllegalArgumentException("Only one bundle can be specified");
			String symbolicName = bundle.isEmpty() ? "org.argeo.cms.cli" : bundle.get(0);

			Path script = plainArgs.isEmpty() ? null : Paths.get(plainArgs.get(0));
			List<String> scriptArgs = new ArrayList<>();
			for (int i = 1; i < plainArgs.size(); i++)
				scriptArgs.add(plainArgs.get(i));

			JShellClient client = new JShellClient(targetStateDirectory, symbolicName, script, scriptArgs);
			client.run();
		} catch (Exception e) {
			e.printStackTrace();
			printHelp(System.err);
		}
	}

	/** Guaranteed to return a non-null list (which may be empty). */
	private static List<String> opt(Map<String, List<String>> options, String shortOpt, String longOpt) {
		List<String> res = new ArrayList<>();
		if (options.get(shortOpt) != null)
			res.addAll(options.get(shortOpt));
		if (options.get(longOpt) != null)
			res.addAll(options.get(longOpt));
		return res;
	}

	public static void printHelp(PrintStream out) {
		out.println("Start a JShell terminal or execute a JShell script in a local Argeo CMS instance");
		out.println("Usage: jshc -d <sockets directory> -b <bundle> [JShell script] [script arguments...]");
		out.println("  -d, --sockets-dir  app directory with UNIX sockets (default to current dir)");
		out.println("  -b, --bundle       bundle to activate and use as context (default to org.argeo.cms.cli)");
		out.println("  -h, --help         this help message");
	}

	// Copied from org.argeo.cms.util.OS
	private static Path getRunDir() {
		Path runDir;
		String xdgRunDir = System.getenv("XDG_RUNTIME_DIR");
		if (xdgRunDir != null) {
			// TODO support multiple names
			runDir = Paths.get(xdgRunDir);
		} else {
			String username = System.getProperty("user.name");
			if (username.equals("root")) {
				runDir = Paths.get("/run");
			} else {
				Path homeDir = Paths.get(System.getProperty("user.home"));
				if (!Files.isWritable(homeDir)) {
					// typically, dameon's home (/usr/sbin) is not writable
					runDir = Paths.get("/tmp/" + username + "/run");
				} else {
					runDir = homeDir.resolve(".cache/argeo");
				}
			}
		}
		return runDir;
	}

	/*
	 * TERMINAL
	 */
	/** Set the terminal to raw mode. */
	protected synchronized void toRawTerminal() {
		boolean isWindows = File.separatorChar == '\\';
		if (isWindows)
			return;
		if (inEclipse)
			return;
		// save current configuration
		ttyConfig = stty("-g");
		if (ttyConfig == null)
			return;
		ttyConfig.trim();
		// set the console to be character-buffered instead of line-buffered
		stty("-icanon min 1");
		// disable character echoing
		stty("-echo");
	}

	/** Restore original terminal configuration. */
	protected synchronized void toOriginalTerminal() {
		if (ttyConfig == null)
			return;
		try {
			stty(ttyConfig);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ttyConfig = null;
	}

	/**
	 * Execute the stty command with the specified arguments against the current
	 * active terminal.
	 */
	protected String stty(String args) {
		List<String> cmd = new ArrayList<>();
		cmd.add("/bin/sh");
		cmd.add("-c");
		cmd.add(sttyExec + " " + args + " < /dev/tty");

		logger.log(TRACE, () -> cmd.toString());

		try {
			ProcessBuilder pb = new ProcessBuilder(cmd);
			Process p = pb.start();
			String firstLine = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
			p.waitFor();
			logger.log(TRACE, () -> firstLine);
			return firstLine;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/*
	 * SCRIPT
	 */
	private class ScriptThread extends Thread {
		private SocketChannel channel;

		public ScriptThread(SocketChannel channel) {
			super("JShell script writer");
			this.channel = channel;
		}

		@Override
		public void run() {
			try {
				if (benchmark)
					System.err.println(ManagementFactory.getRuntimeMXBean().getUptime());
				StringBuilder sb = new StringBuilder();
				if (!scriptArgs.isEmpty()) {
					// additional arguments as $1, $2, etc.
					for (String arg : scriptArgs)
						sb.append('\"').append(arg).append('\"').append(";\n");
				}
				if (sb.length() > 0)
					writeLine(sb);

				try (BufferedReader reader = Files.newBufferedReader(script)) {
					String line;
					lines: while ((line = reader.readLine()) != null) {
						if (line.startsWith("#"))
							continue lines;
						writeLine(line);
					}
				}

				// exit
				if (channel.isConnected())
					writeLine("/exit");
			} catch (IOException e) {
				logger.log(ERROR, "Cannot execute " + script, e);
			}
		}

		/** Not optimal, but performance is not critical here. */
		private void writeLine(Object obj) throws IOException {
			channel.write(ByteBuffer.wrap((obj + "\n").getBytes(UTF_8)));
		}
	}
}

/** Pipe streams to a channel. */
class SocketPipeSource {
	private ReadableByteChannel inChannel;
	private WritableByteChannel outChannel;

	private Thread readThread;
	private Thread forwardThread;

	private int inBufferSize = 1;
	private int outBufferSize = 1024;

	private final String id;
	private final boolean batch;

	private boolean completed = false;

	public SocketPipeSource(String id, boolean batch) {
		this.id = id;
		this.batch = batch;
	}

	public void process(SocketChannel channel) throws IOException {
		if (batch) {
			Integer socketRcvBuf = channel.getOption(StandardSocketOptions.SO_RCVBUF);
			inBufferSize = socketRcvBuf;
			outBufferSize = socketRcvBuf;
		}

		readThread = new Thread(() -> {

			try {
				ByteBuffer buffer = ByteBuffer.allocate(outBufferSize);
				while (true) {
					if (channel.read(buffer) < 0)
						break;
					buffer.flip();
					outChannel.write(buffer);
					buffer.rewind();
				}
			} catch (ClosedByInterruptException e) {
				// silent
			} catch (AsynchronousCloseException e) {
				// silent
			} catch (IOException e) {
				e.printStackTrace();
			}
			markCompleted();
		}, "JShell read " + id);
		readThread.start();

		// TODO make it smarter than a 1 byte buffer
		// we should recognize control characters
		// e.g ^C
//		int c = System.in.read();
//		if (c == 0x1B) {
//			break;
//		}

		if (inChannel != null) {
			forwardThread = new Thread(() -> {
				try {
					ByteBuffer buffer = ByteBuffer.allocate(inBufferSize);
					while (channel.isConnected()) {
						if (inChannel.read(buffer) < 0) {
							System.err.println("in EOF");
							channel.shutdownOutput();
							break;
						}
//			int b = (int) buffer.get(0);
//			if (b == 0x1B) {
//				System.out.println("Ctrl+C");
//			}

						buffer.flip();
						channel.write(buffer);
						buffer.rewind();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}, "JShell write " + id);
			forwardThread.setDaemon(true);
			forwardThread.start();
			// end
			// TODO make it more robust
			// we want to be asynchronous when read only
//			try {
//				// TODO add timeout
//				readThread.join();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}

		}
	}

	public synchronized boolean isCompleted() {
		if (!completed)
			try {
				wait();
			} catch (InterruptedException e) {
				// silent
			}
		return completed;
	}

	protected synchronized void markCompleted() {
		completed = true;
		notifyAll();
	}

	public void shutdown() {
		if (inChannel != null)
			try {
				inChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		try {
			outChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		if (inChannel != null)
//			forwardThread.interrupt();
//		readThread.interrupt();
	}

	public void setInputStream(InputStream in) {
		inChannel = Channels.newChannel(in);
	}

	public void setOutputStream(OutputStream out) {
		outChannel = Channels.newChannel(out);
	}
}
