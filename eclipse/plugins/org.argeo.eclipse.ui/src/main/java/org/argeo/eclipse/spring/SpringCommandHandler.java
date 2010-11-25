/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
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

package org.argeo.eclipse.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.springframework.context.ApplicationContext;

public class SpringCommandHandler implements IHandler {
	private final static Log log = LogFactory
			.getLog(SpringCommandHandler.class);

	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	public void dispose() {
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			if (log.isDebugEnabled())
				log.debug("Execute " + event + " via spring command handler "
						+ this);

			// Find the application context of the bundle defining the command
			// extension
			// ICommandService commandService =
			// (ICommandService)ArgeoUiPlugin.getDefault().getWorkbench().getService(ICommandService.class);
			//		
			// Command command =
			// commandService.getCommand(event.getCommand().getId());
			// log.debug("command=" + command);
			// log.debug("Command id " + command.getId());
			//		
			// Platform.getExtensionRegistry().getE
			//		
			// IExtension extension =
			// Platform.getExtensionRegistry().getExtension(
			// "org.eclipse.ui.commands", command.getId());
			// log.debug("extension=" + extension);
			// String bundleSymbolicName = extension.getContributor().getName();

			// Assume that the defining bundle name is the first part of the
			// command
			// id
			// TODO: make it more flexible and robust
			String commandId = event.getCommand().getId();
			String bundleSymbolicName = commandId.substring(0, commandId
					.lastIndexOf('.'));
			ApplicationContext applicationContext = ApplicationContextTracker
					.getApplicationContext(bundleSymbolicName);

			// retrieve the command via its id
			String beanName = event.getCommand().getId();
			if (!applicationContext.containsBean(beanName))
				throw new ExecutionException("No bean found with name "
						+ beanName + " in bundle " + bundleSymbolicName);
			Object bean = applicationContext.getBean(beanName);

			if (!(bean instanceof IHandler))
				throw new ExecutionException("Bean with name " + beanName
						+ " and class " + bean.getClass()
						+ " does not implement the IHandler interface.");

			IHandler handler = (IHandler) bean;
			return handler.execute(event);
		} catch (Exception e) {
			// TODO: use eclipse error management
			log.error(e);
			return null;
		}
	}

	public boolean isEnabled() {
		return true;
	}

	public boolean isHandled() {
		return true;
	}

	public void removeHandlerListener(IHandlerListener handlerListener) {
	}

}
