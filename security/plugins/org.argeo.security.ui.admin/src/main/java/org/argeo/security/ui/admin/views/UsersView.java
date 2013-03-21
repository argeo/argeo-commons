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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.StaticOperand;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.specific.EclipseUiSpecificUtils;
import org.argeo.eclipse.ui.utils.CommandUtils;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.commands.OpenArgeoUserEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

/** List all users. */
public class UsersView extends ViewPart implements ArgeoNames {
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".adminUsersView";

	private TableViewer viewer;
	private Text filterTxt;
	private final static String FILTER_HELP_MSG = "Type filter criterion "
			+ "separated by a space (on user ID, name and E-mail)";
	private final static Image FILTER_RESET = SecurityAdminPlugin
			.getImageDescriptor("icons/clear.gif").createImage();

	private Session session;

	private UserStructureListener userStructureListener;
	private UserPropertiesListener userPropertiesListener;

	private Font italic;
	private Font bold;

	@Override
	public void createPartControl(Composite parent) {
		italic = EclipseUiUtils.getItalicFont(parent);
		bold = EclipseUiUtils.getBoldFont(parent);

		// Main Layout
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		createFilterPart(parent);

		viewer = createTableViewer(parent);
		EclipseUiSpecificUtils.enableToolTipSupport(viewer);
		viewer.setContentProvider(new UsersContentProvider());
		viewer.addDoubleClickListener(new ViewDoubleClickListener());
		getViewSite().setSelectionProvider(viewer);

		userStructureListener = new UserStructureListener();
		JcrUtils.addListener(session, userStructureListener, Event.NODE_ADDED
				| Event.NODE_REMOVED, ArgeoJcrConstants.PEOPLE_BASE_PATH, null);
		userPropertiesListener = new UserPropertiesListener();
		JcrUtils.addListener(session, userStructureListener,
				Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED
						| Event.PROPERTY_REMOVED,
				ArgeoJcrConstants.PEOPLE_BASE_PATH,
				ArgeoTypes.ARGEO_USER_PROFILE);

		refreshFilteredList();
	}

	protected TableViewer createTableViewer(final Composite parent) {

		Table table = new Table(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		table.setLayoutData(gd);

		TableViewer viewer = new TableViewer(table);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		// pass a mapping between col index and property name to the comparator.
		List<String> propertiesList = new ArrayList<String>();

		// User ID
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("User ID");
		column.getColumn().setWidth(100);
		column.getColumn().addSelectionListener(getSelectionAdapter(0));
		propertiesList.add(ARGEO_USER_ID);
		column.setLabelProvider(new CLProvider() {
			public String getText(Object elem) {
				return getProperty(elem, ARGEO_USER_ID);
				// if (username.equals(session.getUserID()))
				// return "[" + username + "]";
				// else
				// return username;
			}
		});

		// Displayed name
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("Name");
		column.getColumn().setWidth(150);
		column.getColumn().addSelectionListener(getSelectionAdapter(1));
		propertiesList.add(Property.JCR_TITLE);
		column.setLabelProvider(new CLProvider() {
			public String getText(Object elem) {
				return getProperty(elem, Property.JCR_TITLE);
			}
		});

		// E-mail
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("E-mail");
		column.getColumn().setWidth(150);
		column.getColumn().addSelectionListener(getSelectionAdapter(2));
		propertiesList.add(ARGEO_PRIMARY_EMAIL);
		column.setLabelProvider(new CLProvider() {
			public String getText(Object elem) {
				return getProperty(elem, ARGEO_PRIMARY_EMAIL);
			}
		});

		// Description
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("Description");
		column.getColumn().setWidth(200);
		column.getColumn().addSelectionListener(getSelectionAdapter(3));
		propertiesList.add(Property.JCR_DESCRIPTION);
		column.setLabelProvider(new CLProvider() {
			public String getText(Object elem) {
				return getProperty(elem, Property.JCR_DESCRIPTION);
			}
		});

		viewer.setComparator(new UsersViewerComparator(propertiesList));

		return viewer;
	}

	private class CLProvider extends ColumnLabelProvider {

		public String getToolTipText(Object element) {
			return getText(element);
		}

		@Override
		public Font getFont(Object elem) {
			// self
			String username = getProperty(elem, ARGEO_USER_ID);
			if (username.equals(session.getUserID()))
				return bold;

			// disabled
			try {
				Node userProfile = (Node) elem;
				// Node userProfile = userHome.getNode(ARGEO_PROFILE);
				if (!userProfile.getProperty(ARGEO_ENABLED).getBoolean())
					return italic;
				else
					return null;
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot get font for " + username, e);
			}
		}

	}

	private SelectionAdapter getSelectionAdapter(final int index) {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				UsersViewerComparator comparator = (UsersViewerComparator) viewer
						.getComparator();

				if (index == comparator.getSortColumn()) {
					comparator.setAscending(!comparator.isAscending());
				}
				comparator.setSortColumn(index);
				viewer.getTable().setSortColumn(
						viewer.getTable().getColumn(index));
				viewer.getTable().setSortDirection(
						comparator.isAscending() ? SWT.UP : SWT.DOWN);
				viewer.refresh(false);
			}
		};
		return selectionAdapter;
	}

	private class UsersViewerComparator extends ViewerComparator {

		private List<String> propertyList;
		private int sortColumn = 0;
		private boolean ascending = true;
		// use this to enable two levels sort
		@SuppressWarnings("unused")
		private int lastSortColumn = 0;
		@SuppressWarnings("unused")
		private boolean lastAscending = true;

		public UsersViewerComparator(List<String> propertyList) {
			super();
			this.propertyList = propertyList;
		}

		public int compare(Viewer viewer, Object e1, Object e2) {
			String s1 = getProperty(e1, propertyList.get(sortColumn));
			String s2 = getProperty(e2, propertyList.get(sortColumn));
			int result = super.compare(viewer, s1, s2);
			return ascending ? result : (-1) * result;
		}

		/**
		 * @return Returns the sortColumn.
		 */
		public int getSortColumn() {
			return sortColumn;
		}

		/**
		 * @param sortColumn
		 *            The sortColumn to set.
		 */
		public void setSortColumn(int sortColumn) {
			if (this.sortColumn != sortColumn) {
				lastSortColumn = this.sortColumn;
				lastAscending = this.ascending;
				this.sortColumn = sortColumn;
			}
		}

		/**
		 * @return Returns the ascending.
		 */
		public boolean isAscending() {
			return ascending;
		}

		/**
		 * @param ascending
		 *            The ascending to set.
		 */
		public void setAscending(boolean ascending) {
			this.ascending = ascending;
		}
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		JcrUtils.removeListenerQuietly(session, userStructureListener);
		JcrUtils.removeListenerQuietly(session, userPropertiesListener);
		super.dispose();
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void refresh() {
		// viewer.refresh();
		refreshFilteredList();
	}

	protected String getProperty(Node userProfile, String name)
			throws RepositoryException {
		return userProfile.hasProperty(name) ? userProfile.getProperty(name)
				.getString() : "";
	}

	protected String getProperty(Object element, String name) {
		try {
			Node userProfile = (Node) element;
			// Node userProfile = userHome.getNode(ARGEO_PROFILE);
			return userProfile.hasProperty(name) ? userProfile
					.getProperty(name).getString() : "";
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get property " + name, e);
		}
	}

	private class UserStructureListener implements EventListener {

		@Override
		public void onEvent(EventIterator events) {
			// viewer.refresh();
			refreshFilteredList();
		}
	}

	private class UserPropertiesListener implements EventListener {

		@Override
		public void onEvent(EventIterator events) {
			// viewer.refresh();
			refreshFilteredList();
		}
	}

	private class UsersContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;

			// try {
			// Query query = session
			// .getWorkspace()
			// .getQueryManager()
			// .createQuery(
			// "select * from ["
			// + ArgeoTypes.ARGEO_USER_PROFILE + "]",
			// Query.JCR_SQL2);
			// NodeIterator nit = query.execute().getNodes();
			// List<Node> userProfiles = new ArrayList<Node>();
			// while (nit.hasNext()) {
			// userProfiles.add(nit.nextNode());
			// }
			// return userProfiles.toArray();
			// } catch (RepositoryException e) {
			// throw new ArgeoException("Cannot list users", e);
			// }
			// // return userAdminService.listUsers().toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
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

	/* MANAGE FILTER */
	private void createFilterPart(Composite parent) {
		Composite header = new Composite(parent, SWT.FILL);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		header.setLayout(new GridLayout(2, false));

		// Text Area to filter
		filterTxt = new Text(header, SWT.BORDER | SWT.SINGLE);
		filterTxt.setMessage(FILTER_HELP_MSG);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.grabExcessHorizontalSpace = true;
		filterTxt.setLayoutData(gd);
		filterTxt.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				refreshFilteredList();
			}
		});

		Button resetBtn = new Button(header, SWT.PUSH);
		resetBtn.setImage(FILTER_RESET);
		resetBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				resetFilter();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

	}

	private void resetFilter() {
		filterTxt.setText("");
		filterTxt.setMessage(FILTER_HELP_MSG);
	}

	private void refreshFilteredList() {
		List<Node> nodes;
		try {
			nodes = JcrUtils.nodeIteratorToList(listFilteredElements(session,
					filterTxt.getText()));
			viewer.setInput(nodes.toArray());
		} catch (RepositoryException e) {
			throw new ArgeoException("Unable to list users", e);
		}
	}

	/** Build repository request */
	private NodeIterator listFilteredElements(Session session, String filter)
			throws RepositoryException {
		QueryManager queryManager = session.getWorkspace().getQueryManager();
		QueryObjectModelFactory factory = queryManager.getQOMFactory();

		final String bundleArtifactsSelector = "userProfiles";
		Selector source = factory.selector(ArgeoTypes.ARGEO_USER_PROFILE,
				bundleArtifactsSelector);

		// Create a dynamic operand for each property on which we want to filter
		DynamicOperand userIdDO = factory.propertyValue(
				source.getSelectorName(), ARGEO_USER_ID);
		DynamicOperand fullNameDO = factory.propertyValue(
				source.getSelectorName(), Property.JCR_TITLE);
		DynamicOperand mailDO = factory.propertyValue(source.getSelectorName(),
				ARGEO_PRIMARY_EMAIL);

		// Default Constraint: no source artifacts
		Constraint defaultC = null;

		// Build constraints based the textArea content
		if (filter != null && !"".equals(filter.trim())) {
			// Parse the String
			String[] strs = filter.trim().split(" ");
			for (String token : strs) {
				token = token.replace('*', '%');
				StaticOperand so = factory.literal(session.getValueFactory()
						.createValue("%" + token + "%"));

				Constraint currC = factory.comparison(userIdDO,
						QueryObjectModelFactory.JCR_OPERATOR_LIKE, so);
				currC = factory.or(currC, factory.comparison(fullNameDO,
						QueryObjectModelFactory.JCR_OPERATOR_LIKE, so));
				currC = factory.or(currC, factory.comparison(mailDO,
						QueryObjectModelFactory.JCR_OPERATOR_LIKE, so));

				if (defaultC == null)
					defaultC = currC;
				else
					defaultC = factory.and(defaultC, currC);
			}
		}

		Ordering order = factory.ascending(factory.propertyValue(
				bundleArtifactsSelector, ARGEO_USER_ID));
		// Ordering order2 = factory.ascending(factory.propertyValue(
		// bundleArtifactsSelector, ARGEO_PRIMARY_EMAIL));
		// Ordering[] orderings = { order, order2 };
		Ordering[] orderings = { order };

		QueryObjectModel query = factory.createQuery(source, defaultC,
				orderings, null);

		QueryResult result = query.execute();
		return result.getNodes();
	}
}