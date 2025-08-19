package org.argeo.api.cli;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.WARNING;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.System.Logger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.security.auth.callback.CallbackHandler;

//import org.apache.commons.exec.DefaultExecutor;
//import org.apache.commons.exec.ExecuteException;
//import org.apache.commons.exec.ExecuteResultHandler;
//import org.apache.commons.exec.ExecuteStreamHandler;
//import org.apache.commons.exec.ExecuteWatchdog;
//import org.apache.commons.exec.PumpStreamHandler;
//import org.apache.commons.exec.ShutdownHookProcessDestroyer;

/** Execute an OS specific system call. */
public class NativeExec implements Runnable {
	public final static String LOG_STDOUT = "System.out";

	private final Logger logger = System.getLogger(getClass().getName());

	private String execDir;

	private String cmd = null;
	private List<Object> command = null;

//	private Executor executor = new DefaultExecutor();
	private Boolean synchronous = true;

	private String stdErrLogLevel = "ERROR";
	private String stdOutLogLevel = "INFO";

	private Path stdOutFile = null;
	private Path stdErrFile = null;

	private Path stdInFile = null;
	/**
	 * If no {@link #stdInFile} provided, writing to this stream will write to the
	 * stdin of the process.
	 */
	private OutputStream stdInSink = null;

//	private Boolean redirectStdOut = false;

	private List<NativeExecOutputListener> outputListeners = Collections
			.synchronizedList(new ArrayList<NativeExecOutputListener>());

	private Map<String, List<Object>> osCommands = new HashMap<String, List<Object>>();
	private Map<String, String> osCmds = new HashMap<String, String>();
	private Map<String, String> environmentVariables = new HashMap<String, String>();

	private boolean logCommand = false;
	private boolean redirectStreams = true;
//	private boolean exceptionOnFailed = true;
	private boolean mergeEnvironmentVariables = true;

//	private Authentication authentication;

	private String osShell = null;
	private String generateScript = null;

	/** 24 hours */
	private Long synchronousTimeout = 24 * 60 * 60 * 1000l;

	/** Sudo the command, as root if empty or as user if not. */
	private String sudo = null;
	// TODO make it more secure and robust, test only once
	private final String sudoPrompt = UUID.randomUUID().toString();
	private String askPassProgram = "/usr/libexec/openssh/ssh-askpass";
	@SuppressWarnings("unused")
	private boolean firstLine = true;
	@SuppressWarnings("unused")
	private CallbackHandler callbackHandler;
	/** Chroot to the this path (must not be empty) */
	private String chroot = null;

	// Current
	/** Current watchdog, null if process is completed */
//	ExecuteWatchdog currentWatchdog = null;
	private Process currentProcess;
	private CompletableFuture<Process> onExit;

	/** Empty constructor */
	public NativeExec() {

	}

	/**
	 * Constructor based on the provided command list.
	 * 
	 * @param command the command list
	 */
	public NativeExec(List<Object> command) {
		this.command = command;
	}

	/**
	 * Constructor based on the provided command.
	 * 
	 * @param cmd the command. If the provided string contains no space a command
	 *            list is initialized with the argument as first component (useful
	 *            for chained construction)
	 */
	public NativeExec(String cmd) {
		if (cmd.indexOf(' ') < 0) {
			command = new ArrayList<Object>();
			command.add(cmd);
		} else {
			this.cmd = cmd;
		}
	}

	public void run() {
		try {
			execute();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot execute " + toString(), e);
		}
	}

	/** Executes the system call. */
	public void execute() throws IOException {
//		authentication = SecurityContextHolder.getContext().getAuthentication();

		// Manage streams
//		Writer stdOutWriter = null;
//		OutputStream stdOutputStream = null;
//		Writer stdErrWriter = null;
//		InputStream stdInStream = null;
//		if (stdOutFile != null)
//			if (redirectStdOut)
//				stdOutputStream = createOutputStream(stdOutFile);
//			else
//				stdOutWriter = createWriter(stdOutFile);
//
//		if (stdErrFile != null) {
//			stdErrWriter = createWriter(stdErrFile);
//		} else {
//			if (stdOutFile != null && !redirectStdOut)
//				stdErrWriter = createWriter(stdOutFile);
//		}
//
//		try {
//			if (stdInFile != null)
//				stdInStream = Files.newInputStream(stdInFile);
//			else {
//				stdInStream = new PipedInputStream();
//				stdInSink = new PipedOutputStream((PipedInputStream) stdInStream);
//			}
//		} catch (IOException e2) {
//			throw new UncheckedIOException("Cannot open a stream for " + stdInFile, e2);
//		}

		logger.log(TRACE, () -> "os.name=" + System.getProperty("os.name"));
		logger.log(TRACE, () -> "os.arch=" + System.getProperty("os.arch"));
		logger.log(TRACE, () -> "os.version=" + System.getProperty("os.version"));

		// Execution directory
		File dir = new File(getExecDirToUse());
		// if (!dir.exists())
		// dir.mkdirs();

		// Watchdog to check for lost processes
//		Executor executorToUse;
//		if (executor != null)
//			executorToUse = executor;
//		else
//			executorToUse = new DefaultExecutor();
//		executorToUse.setWatchdog(createWatchdog());

		// Command line to use
		final NativeCommandLine commandLine = createCommandLine();
		if (logCommand)
			logger.log(INFO, "Execute command:\n" + commandLine + "\n in working directory: \n" + dir + "\n");

		ProcessBuilder pb = commandLine.toProcessBuilder();

		// TODO test it and clarify
		// TODO if not redirecting, parse out/err and log accordingly
		if (redirectStreams) {
			// Redirect standard streams
			if (stdOutFile != null)
				pb.redirectOutput(stdOutFile.toFile());

			if (stdErrFile != null)
				pb.redirectOutput(stdErrFile.toFile());

			if (stdInFile != null)
				pb.redirectInput(stdInFile.toFile());

//			executorToUse.setStreamHandler(
//					createExecuteStreamHandler(stdOutWriter, stdOutputStream, stdErrWriter, stdInStream));
		}

//		executorToUse.setProcessDestroyer(new ShutdownHookProcessDestroyer());

//		executorToUse.setWorkingDirectory(dir);
		pb.directory(dir);

		// Env variables
		Map<String, String> environmentVariablesToUse = null;
		environmentVariablesToUse = new HashMap<String, String>();
		if (mergeEnvironmentVariables)
			environmentVariablesToUse.putAll(System.getenv());
		if (environmentVariables.size() > 0)
			environmentVariablesToUse.putAll(environmentVariables);

		// Execute
//		ExecuteResultHandler executeResultHandler = createExecuteResultHandler(commandLine);

		pb.environment().putAll(environmentVariablesToUse);

		//
		// THE EXECUTION PROPER
		//
		Process process = pb.start();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (process.isAlive()) {
				process.destroy();
			}
		}));
		currentProcess = process;

		if (currentProcess.getInputStream() != null) {// not redirected
			redirectToListeners(currentProcess.getInputStream(), false);
		}

		if (currentProcess.getErrorStream() != null) {// not redirected
			redirectToListeners(currentProcess.getErrorStream(), true);
		}

		if (stdInFile == null)
			stdInSink = currentProcess.getOutputStream();

		onExit = currentProcess.onExit();

		if (synchronous) {
			try {
				onExit.get(synchronousTimeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				throw new RuntimeException(e);
			}
		}

//		try {
//			if (synchronous)
//				try {
//					int exitValue = executorToUse.execute(commandLine, environmentVariablesToUse);
//					executeResultHandler.onProcessComplete(exitValue);
//				} catch (ExecuteException e1) {
//					if (e1.getExitValue() == Executor.INVALID_EXITVALUE) {
//						Thread.currentThread().interrupt();
//						return;
//					}
//					// Sleep 1s in order to make sure error logs are flushed
//					Thread.sleep(1000);
//					executeResultHandler.onProcessFailed(e1);
//				}
//			else {
//				executorToUse.execute(commandLine, environmentVariablesToUse, executeResultHandler);
//			}
//		}
//		// FIXME better deal with exceptions
//		catch (RuntimeException e) {
//			throw e;
//		} catch (Exception e) {
//			throw new RuntimeException("Could not execute command " + commandLine, e);
//		} finally {
//			IOUtils.closeQuietly(stdOutWriter);
//			IOUtils.closeQuietly(stdErrWriter);
//			IOUtils.closeQuietly(stdInStream);
//			IOUtils.closeQuietly(stdInSink);
//		}

	}

	public synchronized String function() {
		final StringBuffer buf = new StringBuffer("");
		NativeExecOutputListener tempOutputListener = new NativeExecOutputListener() {
			private Long lineCount = 0l;

			public void newLine(NativeExec systemCall, String line, boolean isError) {
				if (!isError) {
					if (lineCount != 0l)
						buf.append('\n');
					buf.append(line);
					lineCount++;
				}
			}
		};
		addOutputListener(tempOutputListener);
		run();
		removeOutputListener(tempOutputListener);
		return buf.toString();
	}

	public String asCommand() {
		try {
			return createCommandLine().toString();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot generate command", e);
		}
	}

	@Override
	public String toString() {
		return asCommand();
	}

	/**
	 * Build a command line based on the properties. Can be overridden by specific
	 * command wrappers.
	 */
	protected NativeCommandLine createCommandLine() throws IOException {
		// Check if an OS specific command overrides
		String osName = System.getProperty("os.name");
		List<Object> commandToUse = null;
		if (osCommands.containsKey(osName))
			commandToUse = osCommands.get(osName);
		else
			commandToUse = command;
		String cmdToUse = null;
		if (osCmds.containsKey(osName))
			cmdToUse = osCmds.get(osName);
		else
			cmdToUse = cmd;

		NativeCommandLine commandLine = null;

		// Which command definition to use
		if (commandToUse == null && cmdToUse == null)
			throw new IllegalArgumentException("Please specify a command.");
		else if (commandToUse != null && cmdToUse != null)
			throw new IllegalArgumentException("Specify the command either as a line or as a list.");
		else if (cmdToUse != null) {
			if (chroot != null && !chroot.trim().equals(""))
				cmdToUse = "chroot \"" + chroot + "\" " + cmdToUse;
			if (sudo != null) {
				environmentVariables.put("SUDO_ASKPASS", askPassProgram);
				if (!sudo.trim().equals(""))
					cmdToUse = "sudo -p " + sudoPrompt + " -u " + sudo + " " + cmdToUse;
				else
					cmdToUse = "sudo -p " + sudoPrompt + " " + cmdToUse;
			}

			// GENERATE COMMAND LINE
			commandLine = NativeCommandLine.parse(cmdToUse);
		} else if (commandToUse != null) {
			if (commandToUse.size() == 0)
				throw new IllegalArgumentException("Command line is empty.");

			if (chroot != null && sudo != null) {
				commandToUse.add(0, "chroot");
				commandToUse.add(1, chroot);
			}

			if (sudo != null) {
				environmentVariables.put("SUDO_ASKPASS", askPassProgram);
				commandToUse.add(0, "sudo");
				commandToUse.add(1, "-p");
				commandToUse.add(2, sudoPrompt);
				if (!sudo.trim().equals("")) {
					commandToUse.add(3, "-u");
					commandToUse.add(4, sudo);
				}
			}

			// GENERATE COMMAND LINE
			commandLine = NativeCommandLine.parse(commandToUse.get(0).toString());

			for (int i = 1; i < commandToUse.size(); i++) {
				if (logger.isLoggable(TRACE))
					logger.log(TRACE, commandToUse.get(i));
				commandLine.addArgument(commandToUse.get(i).toString());
			}
		} else {
			// all cases covered previously
			throw new UnsupportedOperationException();
		}

		if (generateScript != null) {
			Path scriptPath = Paths.get(getExecDirToUse(), generateScript);
			Files.writeString(scriptPath, (osShell != null ? osShell + " " : "") + commandLine.toString());
			File scriptFile = new File(getExecDirToUse() + File.separator + generateScript);
			commandLine = NativeCommandLine.parse(scriptFile.toPath().toAbsolutePath().toString());
		} else {
			if (osShell != null)
				commandLine = NativeCommandLine.parse(osShell + " " + commandLine.toString());
		}

		return commandLine;
	}

	protected void redirectToListeners(InputStream in, boolean isError) {
		// TODO deal properly with encoding
		Charset charset = StandardCharsets.UTF_8;
		String msg = (isError ? "stderr" : "stdout") + " stream of " + NativeExec.this.toString();
		new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (isError)
						logStdErr(line);
					else
						logStdOut(line);
				}
			} catch (IOException e) {
				throw new UncheckedIOException("Cannot redirect " + msg, e);
			}
		}, "Redirect " + msg).start();
	}

//	/**
//	 * Creates a {@link PumpStreamHandler} which redirects streams to the custom
//	 * logging mechanism.
//	 */
//	protected ExecuteStreamHandler createExecuteStreamHandler(final Writer stdOutWriter,
//			final OutputStream stdOutputStream, final Writer stdErrWriter, final InputStream stdInStream) {
//
//		// Log writers
//		OutputStream stdout = stdOutputStream != null ? stdOutputStream : new LogOutputStream() {
//			protected void processLine(String line, int level) {
//				// if (firstLine) {
//				// if (sudo != null && callbackHandler != null
//				// && line.startsWith(sudoPrompt)) {
//				// try {
//				// PasswordCallback pc = new PasswordCallback(
//				// "sudo password", false);
//				// Callback[] cbs = { pc };
//				// callbackHandler.handle(cbs);
//				// char[] pwd = pc.getPassword();
//				// char[] arr = Arrays.copyOf(pwd,
//				// pwd.length + 1);
//				// arr[arr.length - 1] = '\n';
//				// IOUtils.write(arr, stdInSink);
//				// stdInSink.flush();
//				// } catch (Exception e) {
//				// throw new SlcException(
//				// "Cannot retrieve sudo password", e);
//				// }
//				// }
//				// firstLine = false;
//				// }
//
//				if (line != null && !line.trim().equals(""))
//					logStdOut(line);
//
//				if (stdOutWriter != null)
//					appendLineToFile(stdOutWriter, line);
//			}
//		};
//
//		OutputStream stderr = new LogOutputStream() {
//			protected void processLine(String line, int level) {
//				if (line != null && !line.trim().equals(""))
//					logStdErr(line);
//				if (stdErrWriter != null)
//					appendLineToFile(stdErrWriter, line);
//			}
//		};
//
//		PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(stdout, stderr, stdInStream) {
//
//			@Override
//			public void stop() throws IOException {
//				// prevents the method to block when joining stdin
//				if (stdInSink != null)
//					IOUtils.closeQuietly(stdInSink);
//
//				super.stop();
//			}
//		};
//		return pumpStreamHandler;
//	}

	/** Creates the default {@link ExecuteResultHandler}. */
//	protected ExecuteResultHandler createExecuteResultHandler(final CommandLine commandLine) {
//		return new ExecuteResultHandler() {
//
//			public void onProcessComplete(int exitValue) {
//				String msg = "System call '" + commandLine + "' properly completed.";
//				logger.log(TRACE, () -> msg);
//				if (testResult != null) {
//					forwardPath(testResult);
//					testResult.addResultPart(new SimpleResultPart(TestStatus.PASSED, msg));
//				}
//				releaseWatchdog();
//			}
//
//			public void onProcessFailed(ExecuteException e) {
//
//				String msg = "System call '" + commandLine + "' failed.";
//				if (testResult != null) {
//					forwardPath(testResult);
//					testResult.addResultPart(new SimpleResultPart(TestStatus.ERROR, msg, e));
//				} else {
//					if (exceptionOnFailed)
//						throw new SlcException(msg, e);
//					else
//						logger.log(ERROR, msg, e);
//				}
//				releaseWatchdog();
//			}
//		};
//	}

	/**
	 * Shortcut method getting the execDir to use
	 */
	protected String getExecDirToUse() {
		if (execDir != null) {
			return execDir;
		}
		return System.getProperty("user.dir");
	}

	protected void logStdOut(String line) {
		for (NativeExecOutputListener outputListener : outputListeners)
			outputListener.newLine(this, line, false);
		log(stdOutLogLevel, line);
	}

	protected void logStdErr(String line) {
		for (NativeExecOutputListener outputListener : outputListeners)
			outputListener.newLine(this, line, true);
		log(stdErrLogLevel, line);
	}

	/** Log from the underlying streams. */
	private void log(String logLevel, String line) {
		// TODO optimize
//		if (SecurityContextHolder.getContext().getAuthentication() == null) {
//			SecurityContextHolder.getContext()
//					.setAuthentication(authentication);
//		}

		if ("ERROR".equals(logLevel))
			logger.log(ERROR, line);
		else if ("WARN".equals(logLevel))
			logger.log(WARNING, line);
		else if ("WARNING".equals(logLevel))
			logger.log(WARNING, line);
		else if ("INFO".equals(logLevel))
			logger.log(INFO, line);
		else if ("DEBUG".equals(logLevel))
			logger.log(DEBUG, line);
		else if ("TRACE".equals(logLevel))
			logger.log(TRACE, line);
		else if (LOG_STDOUT.equals(logLevel))
			System.out.println(line);
		else if ("System.err".equals(logLevel))
			System.err.println(line);
		else
			throw new IllegalArgumentException("Unknown log level " + logLevel);
	}

	/** Append line to a log file. */
	protected void appendLineToFile(Writer writer, String line) {
		try {
			writer.append(line).append('\n');
		} catch (IOException e) {
			logger.log(ERROR, "Cannot write to log file", e);
		}
	}

	/** Creates the writer for the output/err files. */
	protected Writer createWriter(Path target) throws IOException {
		return Files.newBufferedWriter(target, StandardOpenOption.APPEND);
	}

	/** Creates an outputstream for the output/err files. */
	protected OutputStream createOutputStream(Path target) throws IOException {
		return Files.newOutputStream(target);
	}

	/** Append the argument (for chaining) */
	public NativeExec arg(String arg) {
		if (command == null)
			command = new ArrayList<Object>();
		command.add(arg);
		return this;
	}

	/** Append the argument (for chaining) */
	public NativeExec arg(String arg, String value) {
		if (command == null)
			command = new ArrayList<Object>();
		command.add(arg);
		command.add(value);
		return this;
	}

	// CONTROL
	public synchronized boolean isAlive() {
		return currentProcess != null && currentProcess.isAlive();
	}

//	private synchronized ExecuteWatchdog createWatchdog() {
////		if (currentWatchdog != null)
////			throw new SlcException("A process is already being monitored");
//		currentWatchdog = new ExecuteWatchdog(watchdogTimeout);
//		return currentWatchdog;
//	}

//	private synchronized void releaseWatchdog() {
//		currentWatchdog = null;
//	}

	public synchronized void kill() {
		if (currentProcess != null)
			currentProcess.destroy();
	}

	public Optional<OutputStream> getStdInSink() {
		return stdInSink == null ? Optional.empty() : Optional.of(stdInSink);
	}

	/** */
	public void setCmd(String command) {
		this.cmd = command;
	}

	public void setCommand(List<Object> command) {
		this.command = command;
	}

	public void setExecDir(String execdir) {
		this.execDir = execdir;
	}

	public void setStdErrLogLevel(String stdErrLogLevel) {
		this.stdErrLogLevel = stdErrLogLevel;
	}

	public void setStdOutLogLevel(String stdOutLogLevel) {
		this.stdOutLogLevel = stdOutLogLevel;
	}

	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}

	public void setOsCommands(Map<String, List<Object>> osCommands) {
		this.osCommands = osCommands;
	}

	public void setOsCmds(Map<String, String> osCmds) {
		this.osCmds = osCmds;
	}

	public void setEnvironmentVariables(Map<String, String> environmentVariables) {
		this.environmentVariables = environmentVariables;
	}

	public Map<String, String> getEnvironmentVariables() {
		return environmentVariables;
	}

	public void setSynchronousTimeout(long watchdogTimeout) {
		this.synchronousTimeout = watchdogTimeout;
	}

	public void setStdOutFile(Path stdOutFile) {
		this.stdOutFile = stdOutFile;
	}

	public void setStdErrFile(Path stdErrFile) {
		this.stdErrFile = stdErrFile;
	}

	public void setStdInFile(Path stdInFile) {
		this.stdInFile = stdInFile;
	}

	public void setLogCommand(boolean logCommand) {
		this.logCommand = logCommand;
	}

	public void setRedirectStreams(Boolean redirectStreams) {
		this.redirectStreams = redirectStreams;
	}

//	public void setExceptionOnFailed(Boolean exceptionOnFailed) {
//		this.exceptionOnFailed = exceptionOnFailed;
//	}

	public void setMergeEnvironmentVariables(Boolean mergeEnvironmentVariables) {
		this.mergeEnvironmentVariables = mergeEnvironmentVariables;
	}

	public void setOsShell(String osShell) {
		this.osShell = osShell;
	}

	public void setGenerateScript(String generateScript) {
		this.generateScript = generateScript;
	}

//	public void setRedirectStdOut(Boolean redirectStdOut) {
//		this.redirectStdOut = redirectStdOut;
//	}

	public void addOutputListener(NativeExecOutputListener outputListener) {
		outputListeners.add(outputListener);
	}

	public void removeOutputListener(NativeExecOutputListener outputListener) {
		outputListeners.remove(outputListener);
	}

	public void setOutputListeners(List<NativeExecOutputListener> outputListeners) {
		this.outputListeners = outputListeners;
	}

	public void setSudo(String sudo) {
		this.sudo = sudo;
	}

	public void setCallbackHandler(CallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public void setChroot(String chroot) {
		this.chroot = chroot;
	}

//	private class DummyexecuteStreamHandler implements ExecuteStreamHandler {
//
//		public void setProcessErrorStream(InputStream is) throws IOException {
//		}
//
//		public void setProcessInputStream(OutputStream os) throws IOException {
//		}
//
//		public void setProcessOutputStream(InputStream is) throws IOException {
//		}
//
//		public void start() throws IOException {
//		}
//
//		public void stop() {
//		}
//
//	}
}
