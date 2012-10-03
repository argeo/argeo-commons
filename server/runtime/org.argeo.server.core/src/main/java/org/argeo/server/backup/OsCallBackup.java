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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.argeo.ArgeoException;

/**
 * Runs a an OS command and save the output as a file. Typically used for MySQL
 * dumps
 */
public class OsCallBackup implements Runnable {

	private final static Log log = LogFactory.getLog(OsCallBackup.class);

	private String command;
	private Map<String, String> variables = new HashMap<String, String>();

	private String target;

	@Override
	public void run() {
		try {
			Executor executor = new DefaultExecutor();

			CommandLine commandLine = CommandLine.parse(command, variables);

			// stdout
			FileSystemManager fsm = VFS.getManager();
			FileObject targetFo = fsm.resolveFile(target);
			FileContent targetContent = targetFo.getContent();

			// stderr
			ByteArrayOutputStream errBos = new ByteArrayOutputStream();
			ExecuteStreamHandler streamHandler = new PumpStreamHandler(
					targetContent.getOutputStream(), errBos);

			executor.setStreamHandler(streamHandler);

			try {
				if (log.isDebugEnabled())
					log.debug(commandLine.toString());

				executor.execute(commandLine);
			} catch (ExecuteException e) {
				byte[] err = errBos.toByteArray();
				String errStr = new String(err);
				throw new ArgeoException("Process failed with exit value "
						+ e.getExitValue() + ": " + errStr);
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot backup to " + target
					+ " with command " + command + " " + variables, e);
		}
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public void setVariables(Map<String, String> variables) {
		this.variables = variables;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public static void main(String args[]) {
		OsCallBackup osCallBackup = new OsCallBackup();
		osCallBackup.setCommand("/usr/bin/mysqldump"
				+ " --lock-tables --add-locks --add-drop-table"
				+ " -u ${dbUser} --password=${dbPassword} --databases ${dbName}");
		Map<String, String> variables = new HashMap<String, String>();
		variables.put("dbUser", "root");
		variables.put("dbPassword", "");
		variables.put("dbName", "test");
		osCallBackup.setVariables(variables);

		osCallBackup
				.setTarget("/home/mbaudier/dev/src/commons/server/runtime/org.argeo.server.core/target/dump.sql");

		osCallBackup.run();
	}
}
