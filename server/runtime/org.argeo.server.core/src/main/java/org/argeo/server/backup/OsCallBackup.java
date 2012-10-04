package org.argeo.server.backup;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.argeo.ArgeoException;

/**
 * Runs an OS command and save its standard output as a file. Typically used for
 * MySQL or OpenLDAP dumps.
 */
public class OsCallBackup extends AbstractAtomicBackup {
	private final static Log log = LogFactory.getLog(OsCallBackup.class);

	private String command;
	private Map<String, String> variables = new HashMap<String, String>();
	private Executor executor = new DefaultExecutor();

	public OsCallBackup() {
	}

	public OsCallBackup(String name) {
		super(name);
	}

	public OsCallBackup(String name, String command) {
		super(name);
		this.command = command;
	}

	@Override
	public void writeBackup(FileObject targetFo) {
		CommandLine commandLine = CommandLine.parse(command, variables);
		ByteArrayOutputStream errBos = new ByteArrayOutputStream();
		if (log.isTraceEnabled())
			log.trace(commandLine.toString());

		try {
			// stdout
			FileContent targetContent = targetFo.getContent();
			// stderr
			ExecuteStreamHandler streamHandler = new PumpStreamHandler(
					targetContent.getOutputStream(), errBos);
			executor.setStreamHandler(streamHandler);
			executor.execute(commandLine);
		} catch (ExecuteException e) {
			byte[] err = errBos.toByteArray();
			String errStr = new String(err);
			throw new ArgeoException("Process " + commandLine
					+ " failed with exit value " + e.getExitValue() + ": "
					+ errStr, e);
		} catch (Exception e) {
			byte[] err = errBos.toByteArray();
			String errStr = new String(err);
			throw new ArgeoException("Process " + commandLine + " failed: "
					+ errStr, e);
		} finally {
			IOUtils.closeQuietly(errBos);
		}
	}

	public void setCommand(String command) {
		this.command = command;
	}

	protected String getCommand() {
		return command;
	}

	protected Map<String, String> getVariables() {
		return variables;
	}

	public void setVariables(Map<String, String> variables) {
		this.variables = variables;
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

}
