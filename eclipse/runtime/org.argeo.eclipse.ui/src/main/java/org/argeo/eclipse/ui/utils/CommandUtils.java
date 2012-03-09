/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.eclipse.ui.utils;

import java.util.ArrayList;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.ArgeoUiPlugin;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * Centralizes useful and generic methods concerning eclipse commands.
 * 
 */
public class CommandUtils {

	/**
	 * Factorizes command call that is quite verbose and always the same
	 * 
	 * NOTE that none of the parameter can be null
	 */
	public static void CallCommandWithOneParameter(String commandId,
			String paramId, String paramValue) {
		try {
			IWorkbench iw = ArgeoUiPlugin.getDefault().getWorkbench();

			IHandlerService handlerService = (IHandlerService) iw
					.getService(IHandlerService.class);

			// get the command from plugin.xml
			IWorkbenchWindow window = iw.getActiveWorkbenchWindow();
			ICommandService cmdService = (ICommandService) window
					.getService(ICommandService.class);

			Command cmd = cmdService.getCommand(commandId);

			ArrayList<Parameterization> parameters = new ArrayList<Parameterization>();

			// get the parameter
			IParameter iparam = cmd.getParameter(paramId);

			Parameterization params = new Parameterization(iparam, paramValue);
			parameters.add(params);

			// build the parameterized command
			ParameterizedCommand pc = new ParameterizedCommand(cmd,
					parameters.toArray(new Parameterization[parameters.size()]));

			// execute the command
			handlerService = (IHandlerService) window
					.getService(IHandlerService.class);
			handlerService.executeCommand(pc, null);
		} catch (Exception e) {
			throw new ArgeoException("Error while calling command of id :"
					+ commandId, e);
		}
	}
}
