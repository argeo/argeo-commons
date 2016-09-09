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
package org.argeo.eclipse.ui.workbench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.argeo.eclipse.ui.EclipseUiException;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Centralises useful and generic methods when dealing with commands in an
 * Eclipse Workbench context
 */
public class CommandUtils {

	/**
	 * Commodities the refresh of a single command with no parameter in a
	 * Menu.aboutToShow method to simplify further development
	 * 
	 * Note: that this method should be called with a false show command flag to
	 * remove a contribution that have been previously contributed
	 * 
	 * @param menuManager
	 * @param locator
	 * @param cmdId
	 * @param label
	 * @param icon
	 * @param showCommand
	 */
	public static void refreshCommand(IMenuManager menuManager,
			IServiceLocator locator, String cmdId, String label,
			ImageDescriptor icon, boolean showCommand) {
		refreshParameterizedCommand(menuManager, locator, cmdId, label, icon,
				showCommand, null);
	}

	/**
	 * Commodities the refresh the contribution of a command with a map of
	 * parameters in a context menu
	 * 
	 * The command ID is used has contribution item ID
	 * 
	 * @param menuManager
	 * @param locator
	 * @param cmdId
	 * @param label
	 * @param iconPath
	 * @param showCommand
	 */
	public static void refreshParameterizedCommand(IMenuManager menuManager,
			IServiceLocator locator, String cmdId, String label,
			ImageDescriptor icon, boolean showCommand,
			Map<String, String> params) {
		refreshParameterizedCommand(menuManager, locator, cmdId, cmdId, label,
				icon, showCommand, params);
	}

	/**
	 * Commodities the refresh the contribution of a command with a map of
	 * parameters in a context menu
	 * 
	 * @param menuManager
	 * @param locator
	 * @param contributionId
	 * @param commandId
	 * @param label
	 * @param icon
	 * @param showCommand
	 * @param params
	 */
	public static void refreshParameterizedCommand(IMenuManager menuManager,
			IServiceLocator locator, String contributionId, String commandId,
			String label, ImageDescriptor icon, boolean showCommand,
			Map<String, String> params) {
		IContributionItem ici = menuManager.find(contributionId);
		if (ici != null)
			menuManager.remove(ici);
		if (showCommand) {
			CommandContributionItemParameter contributionItemParameter = new CommandContributionItemParameter(
					locator, null, commandId, SWT.PUSH);

			// Set Params
			contributionItemParameter.label = label;
			contributionItemParameter.icon = icon;

			if (params != null)
				contributionItemParameter.parameters = params;

			CommandContributionItem cci = new CommandContributionItem(
					contributionItemParameter);
			cci.setId(contributionId);
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
	 *            a map that links various command IDs with corresponding String
	 *            values.
	 */
	public static void callCommand(String commandID,
			Map<String, String> paramMap) {
		try {
			IWorkbench iw = WorkbenchUiPlugin.getDefault().getWorkbench();
			IHandlerService handlerService = (IHandlerService) iw
					.getService(IHandlerService.class);
			ICommandService cmdService = (ICommandService) iw
					.getActiveWorkbenchWindow().getService(
							ICommandService.class);
			Command cmd = cmdService.getCommand(commandID);

			ArrayList<Parameterization> parameters = null;
			ParameterizedCommand pc;

			if (paramMap != null) {
				// Set parameters of the command to launch :
				parameters = new ArrayList<Parameterization>();
				Parameterization parameterization;

				for (String id : paramMap.keySet()) {
					parameterization = new Parameterization(
							cmd.getParameter(id), paramMap.get(id));
					parameters.add(parameterization);
				}
				pc = new ParameterizedCommand(cmd,
						parameters.toArray(new Parameterization[parameters
								.size()]));
			} else
				pc = new ParameterizedCommand(cmd, null);

			// execute the command
			handlerService.executeCommand(pc, null);
		} catch (Exception e) {
			throw new EclipseUiException("Unexpected error while"
					+ " calling the command " + commandID, e);
		}
	}
}