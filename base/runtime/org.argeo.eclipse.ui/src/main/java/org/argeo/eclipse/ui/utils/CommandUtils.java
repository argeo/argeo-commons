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
package org.argeo.eclipse.ui.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.ArgeoUiPlugin;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;

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

	/**
	 * Commodities the refresh of a single command with no parameter in a
	 * Menu.aboutToShow method to simplify further development
	 * 
	 * @param menuManager
	 * @param locator
	 * @param cmdId
	 * @param label
	 * @param iconPath
	 * @param showCommand
	 */
	public static void refreshCommand(IMenuManager menuManager,
			IServiceLocator locator, String cmdId, String label,
			ImageDescriptor icon, boolean showCommand) {
		refreshParametrizedCommand(menuManager, locator, cmdId, label, icon,
				showCommand, null);
	}

	/**
	 * Commodities the refresh of a single command with a map of parameters in a
	 * Menu.aboutToShow method to simplify further development
	 * 
	 * @param menuManager
	 * @param locator
	 * @param cmdId
	 * @param label
	 * @param iconPath
	 * @param showCommand
	 */
	public static void refreshParametrizedCommand(IMenuManager menuManager,
			IServiceLocator locator, String cmdId, String label,
			ImageDescriptor icon, boolean showCommand,
			Map<String, String> params) {
		IContributionItem ici = menuManager.find(cmdId);
		if (ici != null)
			menuManager.remove(ici);
		CommandContributionItemParameter contributionItemParameter = new CommandContributionItemParameter(
				locator, null, cmdId, SWT.PUSH);

		if (showCommand) {
			// Set Params
			contributionItemParameter.label = label;
			contributionItemParameter.icon = icon;

			if (params != null)
				contributionItemParameter.parameters = params;

			CommandContributionItem cci = new CommandContributionItem(
					contributionItemParameter);
			cci.setId(cmdId);
			menuManager.add(cci);
		}
	}

	/** Helper to call a command without parameter easily */
	public static void callCommand(String commandID) {
		callCommand(commandID, null);
	}

	/** Helper to call a command with a single parameter easily */
	public static void callCommand(String commandID, String parameterID,
			String parameterValue) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(parameterID, parameterValue);
		callCommand(commandID, params);
	}

	/**
	 * Helper to call a command with a map of parameters easily
	 * 
	 * @param paramMap
	 *            a map that links various commands ids with corresponding
	 *            String values.
	 */
	public static void callCommand(String commandID,
			Map<String, String> paramMap) {
		try {
			IWorkbench iw = ArgeoUiPlugin.getDefault().getWorkbench();
			IHandlerService handlerService = (IHandlerService) iw
					.getService(IHandlerService.class);
			ICommandService cmdService = (ICommandService) iw
					.getActiveWorkbenchWindow().getService(
							ICommandService.class);
			Command cmd = cmdService.getCommand(commandID);

			ArrayList<Parameterization> parameters = null;
			if (paramMap != null) {
				// Set parameters of the command to launch :
				parameters = new ArrayList<Parameterization>();
				Parameterization parameterization;

				for (String id : paramMap.keySet()) {
					parameterization = new Parameterization(
							cmd.getParameter(id), paramMap.get(id));
					parameters.add(parameterization);
				}
			}
			// build the parameterized command
			ParameterizedCommand pc = new ParameterizedCommand(cmd,
					parameters.toArray(new Parameterization[parameters.size()]));
			// execute the command
			handlerService.executeCommand(pc, null);
		} catch (Exception e) {
			throw new ArgeoException(
					"Unexepected exception while opening node editor", e);
		}
	}
}