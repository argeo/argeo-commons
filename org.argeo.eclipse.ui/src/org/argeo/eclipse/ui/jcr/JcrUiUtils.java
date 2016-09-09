package org.argeo.eclipse.ui.jcr;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.jcr.lists.NodeViewerComparator;
import org.argeo.eclipse.ui.jcr.lists.RowViewerComparator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;

/** Utility methods to simplify UI development using SWT (or RWT), jface  and JCR. */
public class JcrUiUtils {

	/**
	 * Centralizes management of updating property value. Among other to avoid
	 * infinite loop when the new value is the same as the ones that is already
	 * stored in JCR.
	 * 
	 * @return true if the value as changed
	 */
	public static boolean setJcrProperty(Node node, String propName,
			int propertyType, Object value) {
		try {
			switch (propertyType) {
			case PropertyType.STRING:
				if ("".equals((String) value)
						&& (!node.hasProperty(propName) || node
								.hasProperty(propName)
								&& "".equals(node.getProperty(propName)
										.getString())))
					// workaround the fact that the Text widget value cannot be
					// set to null
					return false;
				else if (node.hasProperty(propName)
						&& node.getProperty(propName).getString()
								.equals((String) value))
					// nothing changed yet
					return false;
				else {
					node.setProperty(propName, (String) value);
					return true;
				}
			case PropertyType.BOOLEAN:
				if (node.hasProperty(propName)
						&& node.getProperty(propName).getBoolean() == (Boolean) value)
					// nothing changed yet
					return false;
				else {
					node.setProperty(propName, (Boolean) value);
					return true;
				}
			case PropertyType.DATE:
				if (node.hasProperty(propName)
						&& node.getProperty(propName).getDate()
								.equals((Calendar) value))
					// nothing changed yet
					return false;
				else {
					node.setProperty(propName, (Calendar) value);
					return true;
				}
			case PropertyType.LONG:
				Long lgValue = (Long) value;

				if (lgValue == null)
					lgValue = 0L;

				if (node.hasProperty(propName)
						&& node.getProperty(propName).getLong() == lgValue)
					// nothing changed yet
					return false;
				else {
					node.setProperty(propName, lgValue);
					return true;
				}

			default:
				throw new EclipseUiException("Unimplemented property save");
			}
		} catch (RepositoryException re) {
			throw new EclipseUiException("Unexpected error while setting property",
					re);
		}
	}

	/**
	 * Creates a new selection adapter in order to provide sorting abitily on a
	 * SWT Table that display a row list
	 **/
	public static SelectionAdapter getRowSelectionAdapter(final int index,
			final int propertyType, final String selectorName,
			final String propertyName, final RowViewerComparator comparator,
			final TableViewer viewer) {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			private static final long serialVersionUID = -5738918304901437720L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				Table table = viewer.getTable();
				comparator.setColumn(propertyType, selectorName, propertyName);
				int dir = table.getSortDirection();
				if (table.getSortColumn() == table.getColumn(index)) {
					dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
				} else {
					dir = SWT.DOWN;
				}
				table.setSortDirection(dir);
				table.setSortColumn(table.getColumn(index));
				viewer.refresh();
			}
		};
		return selectionAdapter;
	}

	/**
	 * Creates a new selection adapter in order to provide sorting abitily on a
	 * swt table that display a row list
	 **/
	public static SelectionAdapter getNodeSelectionAdapter(final int index,
			final int propertyType, final String propertyName,
			final NodeViewerComparator comparator, final TableViewer viewer) {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			private static final long serialVersionUID = -1683220869195484625L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				Table table = viewer.getTable();
				comparator.setColumn(propertyType, propertyName);
				int dir = table.getSortDirection();
				if (table.getSortColumn() == table.getColumn(index)) {
					dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
				} else {
					dir = SWT.DOWN;
				}
				table.setSortDirection(dir);
				table.setSortColumn(table.getColumn(index));
				viewer.refresh();
			}
		};
		return selectionAdapter;
	}
}
