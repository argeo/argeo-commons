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
package org.argeo.security.ui.admin.views;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.parts.UsersTable;
import org.argeo.eclipse.ui.workbench.CommandUtils;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.commands.OpenArgeoUserEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

/** List all users with filter. */
public class JcrUsersView extends ViewPart implements ArgeoNames {
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".jcrUsersView";

	/* DEPENDENCY INJECTION */
	private Session session;

	private UsersTable userTableCmp;
	private JcrUserListener userStructureListener;
	private JcrUserListener userPropertiesListener;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		// Create the composite that displays the list and a filter
		userTableCmp = new UsersTable(parent, SWT.NO_FOCUS, session);
		userTableCmp.populate(true, false);

		// Configure
		userTableCmp.getTableViewer().addDoubleClickListener(
				new ViewDoubleClickListener());
		getViewSite().setSelectionProvider(userTableCmp.getTableViewer());

		// Add listener to refresh the list when something changes
		userStructureListener = new JcrUserListener(getSite().getShell()
				.getDisplay());
		JcrUtils.addListener(session, userStructureListener, Event.NODE_ADDED
				| Event.NODE_REMOVED, ArgeoJcrConstants.PEOPLE_BASE_PATH, null);
		userPropertiesListener = new JcrUserListener(getSite().getShell()
				.getDisplay());
		JcrUtils.addListener(session, userStructureListener,
				Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED
						| Event.PROPERTY_REMOVED,
				ArgeoJcrConstants.PEOPLE_BASE_PATH,
				ArgeoTypes.ARGEO_USER_PROFILE);
	}

	@Override
	public void setFocus() {
		userTableCmp.setFocus();
	}

	@Override
	public void dispose() {
		JcrUtils.removeListenerQuietly(session, userStructureListener);
		JcrUtils.removeListenerQuietly(session, userPropertiesListener);
		JcrUtils.logoutQuietly(session);
		super.dispose();
	}

	// public void setSession(Session session) {
	// this.session = session;
	// }

	public void refresh() {
		this.getSite().getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				userTableCmp.refresh();
			}
		});
	}

	private class JcrUserListener implements EventListener {
		private final Display display;

		public JcrUserListener(Display display) {
			super();
			this.display = display;
		}

		@Override
		public void onEvent(EventIterator events) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					userTableCmp.refresh();
				}
			});
		}
	}

	class ViewDoubleClickListener implements IDoubleClickListener {
		public void doubleClick(DoubleClickEvent evt) {
			if (evt.getSelection().isEmpty())
				return;

			Object obj = ((IStructuredSelection) evt.getSelection())
					.getFirstElement();
			if (obj instanceof Node) {
				try {
					String username = ((Node) obj).getProperty(ARGEO_USER_ID)
							.getString();
					String commandId = OpenArgeoUserEditor.COMMAND_ID;
					String paramName = OpenArgeoUserEditor.PARAM_USERNAME;
					CommandUtils.callCommand(commandId, paramName, username);
				} catch (RepositoryException e) {
					throw new ArgeoException("Cannot open user editor", e);
				}
			}
		}
	}

	/* DEPENDENCY INJECTION */
	public void setRepository(Repository repository) {
		try {
			session = repository.login();
		} catch (RepositoryException re) {
			throw new ArgeoException("Unable to initialise local session", re);
		}
	}
}