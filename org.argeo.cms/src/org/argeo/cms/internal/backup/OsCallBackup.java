/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.cms.internal.backup;

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
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
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

	private Map<String, String> environment = new HashMap<String, String>();

	/** Name of the sudo user, root if "", not sudo if null */
	private String sudo = null;

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
		String commandToUse = command;

		// sudo
		if (sudo != null) {
			if (sudo.equals(""))
				commandToUse = "sudo " + commandToUse;
			else
				commandToUse = "sudo -u " + sudo + " " + commandToUse;
		}

		CommandLine commandLine = CommandLine.parse(commandToUse, variables);
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
			executor.execute(commandLine, environment);
		} catch (ExecuteException e) {
			byte[] err = errBos.toByteArray();
			String errStr = new String(err);
			throw new ArgeoException("Process " + commandLine + " failed ("
					+ e.getExitValue() + "): " + errStr, e);
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

	/**
	 * A reference to the environment variables that will be passed to the
	 * process. Empty by default.
	 */
	protected Map<String, String> getEnvironment() {
		return environment;
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

	public void setSudo(String sudo) {
		this.sudo = sudo;
	}

}
