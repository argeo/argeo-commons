package org.argeo.eclipse.ui.jcr.lists;

import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.argeo.ArgeoException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

/**
 * Base comparator to enable ordering on Table or Tree viewer that display Jcr
 * Nodes.
 * 
 * Note that the following snippet must be added before setting the comparator
 * to the corresponding control: <code>
 * // IMPORTANT: initialize comparator before setting it
 * ColumnDefinition firstCol = colDefs.get(0);
 * comparator.setColumn(firstCol.getPropertyType(),
 * firstCol.getPropertyName());
 * viewer.setComparator(comparator); </code>
 */
public class NodeViewerComparator extends ViewerComparator {
	private static final long serialVersionUID = -7782916140737279027L;

	protected String propertyName;

	protected int propertyType;
	public static final int ASCENDING = 0, DESCENDING = 1;
	protected int direction = DESCENDING;

	public NodeViewerComparator() {
	}

	/**
	 * e1 and e2 must both be Jcr nodes.
	 * 
	 * @param viewer
	 * @param e1
	 * @param e2
	 * @return
	 */
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int rc = 0;
		long lc = 0;

		try {

			Node n1 = (Node) e1;
			Node n2 = (Node) e2;

			Value v1 = null;
			Value v2 = null;
			if (n1.hasProperty(propertyName))
				v1 = n1.getProperty(propertyName).getValue();
			if (n2.hasProperty(propertyName))
				v2 = n2.getProperty(propertyName).getValue();

			if (v2 == null && v1 == null)
				return 0;
			else if (v2 == null)
				return -1;
			else if (v1 == null)
				return 1;

			switch (propertyType) {
			case PropertyType.STRING:
				rc = v1.getString().compareTo(v2.getString());
				break;
			case PropertyType.BOOLEAN:
				boolean b1 = v1.getBoolean();
				boolean b2 = v2.getBoolean();
				if (b1 == b2)
					rc = 0;
				else
					// we assume true is greater than false
					rc = b1 ? 1 : -1;
				break;
			case PropertyType.DATE:
				Calendar c1 = v1.getDate();
				Calendar c2 = v2.getDate();
				if (c1 == null || c2 == null)
					// log.trace("undefined date");
					;
				lc = c1.getTimeInMillis() - c2.getTimeInMillis();
				if (lc < Integer.MIN_VALUE)
					// rc = Integer.MIN_VALUE;
					rc = -1;
				else if (lc > Integer.MAX_VALUE)
					// rc = Integer.MAX_VALUE;
					rc = 1;
				else
					rc = (int) lc;
				break;
			case PropertyType.LONG:
				long l1;
				long l2;
				// FIXME sometimes an empty string is set instead of a long
				try {
					l1 = v1.getLong();
				} catch (ValueFormatException ve) {
					l1 = 0;
				}
				try {
					l2 = v2.getLong();
				} catch (ValueFormatException ve) {
					l2 = 0;
				}

				lc = l1 - l2;
				if (lc < Integer.MIN_VALUE)
					rc = -1;
				else if (lc > Integer.MAX_VALUE)
					rc = 1;
				else
					rc = (int) lc;
				break;
			case PropertyType.DECIMAL:
				BigDecimal bd1 = v1.getDecimal();
				BigDecimal bd2 = v2.getDecimal();
				rc = bd1.compareTo(bd2);
				break;
			case PropertyType.DOUBLE:
				Double d1 = v1.getDouble();
				Double d2 = v2.getDouble();
				rc = d1.compareTo(d2);
				break;
			default:
				throw new ArgeoException(
						"Unimplemented comparaison for PropertyType "
								+ propertyType);
			}
			// If descending order, flip the direction
			if (direction == DESCENDING) {
				rc = -rc;
			}

		} catch (RepositoryException re) {
			throw new ArgeoException("Unexpected error "
					+ "while comparing nodes", re);
		}
		return rc;
	}

	/**
	 * @param propertyType
	 *            Corresponding JCR type
	 * @param propertyName
	 *            name of the property to use.
	 */
	public void setColumn(int propertyType, String propertyName) {
		if (this.propertyName != null && this.propertyName.equals(propertyName)) {
			// Same column as last sort; toggle the direction
			direction = 1 - direction;
		} else {
			// New column; do an ascending sort
			this.propertyType = propertyType;
			this.propertyName = propertyName;
			direction = ASCENDING;
		}
	}

	// Getters and setters
	protected String getPropertyName() {
		return propertyName;
	}

	protected void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	protected int getPropertyType() {
		return propertyType;
	}

	protected void setPropertyType(int propertyType) {
		this.propertyType = propertyType;
	}

	protected int getDirection() {
		return direction;
	}

	protected void setDirection(int direction) {
		this.direction = direction;
	}
}