package org.argeo.server.jxl.dao;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jxl.Cell;
import jxl.CellType;
import jxl.FormulaCell;
import jxl.JXLException;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.ArgeoServerException;
import org.argeo.server.dao.AbstractTabularDaoSupport;
import org.argeo.server.dao.LightDaoSupport;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

public class JxlDaoSupport extends AbstractTabularDaoSupport implements
		LightDaoSupport, ApplicationContextAware, InitializingBean {
	private final static Log log = LogFactory.getLog(JxlDaoSupport.class);

	private Integer charset = 0;

	protected void load(InputStream in, List<Reference> references) {
		try {
			WorkbookSettings workbookSettings = new WorkbookSettings();
			workbookSettings.setCharacterSet(charset);
			Workbook workbook = Workbook.getWorkbook(in, workbookSettings);
			for (Sheet sheet : workbook.getSheets()) {
				loadSheet(sheet, references);
			}
		} catch (Exception e) {
			throw new ArgeoServerException("Cannot load workbook", e);
		}
	}

	protected void loadSheet(Sheet sheet, List<Reference> references)
			throws JXLException {
		if (log.isTraceEnabled())
			log.debug("Instantiate sheet " + sheet.getName());

		Cell[] firstRow = sheet.getRow(0);

		Class<?> clss = findClassToInstantiate(sheet);
		// model.put(clss, new TreeMap<Object, Object>());

		// tempRefs.put(sheet.getName(), new ArrayList<Object>());

		String keyProperty = firstRow[0].getContents();

		if (keyProperty.charAt(keyProperty.length() - 1) == '>') {
			loadAsColumns(clss, keyProperty.substring(0,
					keyProperty.length() - 1), sheet, firstRow, references);
		} else {
			loadAsRows(clss, keyProperty, sheet, firstRow, references);
		}
	}

	protected void loadAsRows(Class<?> clss, String keyProperty, Sheet sheet,
			Cell[] firstRow, List<Reference> references) throws JXLException {
		for (int row = 1; row < sheet.getRows(); row++) {
			if (log.isTraceEnabled())
				log.trace(" row " + row);

			Cell[] currentRow = sheet.getRow(row);
			BeanWrapper bw = newBeanWrapper(clss);
			cells: for (int col = 0; col < firstRow.length; col++) {
				String pName = firstRow[col].getContents();

				if (col < currentRow.length) {
					Cell cell = currentRow[col];
					if (overrideCell(cell, bw, pName, keyProperty, row,
							references))
						continue cells;
					loadCell(cell, bw, pName, keyProperty, row, references);
				}
			}// cells

			saveOrUpdate(bw.getPropertyValue(keyProperty), bw
					.getWrappedInstance(), clss);
			// tempRefs.get(sheet.getName()).add(bw.getWrappedInstance());
			registerInTabularView(sheet.getName(), bw.getWrappedInstance());
		}
	}

	protected void loadAsColumns(Class<?> clss, String keyProperty,
			Sheet sheet, Cell[] firstRow, List<Reference> references)
			throws JXLException {
		Cell[] firstColumn = sheet.getColumn(0);

		for (int col = 1; col < firstRow.length; col++) {
			if (log.isTraceEnabled())
				log.trace(" column " + col);
			BeanWrapper bw = newBeanWrapper(clss);
			Cell[] column = sheet.getColumn(col);
			for (int row = 0; row < column.length; row++) {
				Cell cell = column[row];

				String propertyName;
				if (row == 0)
					propertyName = keyProperty;
				else
					propertyName = firstColumn[row].getContents();

				Class<?> rowType = bw.getPropertyType(propertyName);
				if (log.isTraceEnabled())
					log.trace(" " + propertyName + " rowType="
							+ rowType.getName());
				if (Map.class.isAssignableFrom(rowType)) {
					if (log.isTraceEnabled())
						log.trace("  start building map " + propertyName);
					row++;
					Map<Object, Object> map = new HashMap<Object, Object>();
					String firstColContents = firstColumn[row].getContents();
					mapRows: for (; row < column.length; row++) {
						cell = column[row];

						Object key = firstColContents;
						CellType type = cell.getType();
						if (log.isTraceEnabled())
							log.trace("   row=" + row + ", firstColContents="
									+ firstColContents + ", key=" + key
									+ ", type=" + type);
						if (type.equals(CellType.NUMBER)) {
							map
									.put(key, Double.parseDouble(cell
											.getContents()));
						} else {
							map.put(key, cell.getContents());
						}

						// check next row too see if one should break
						if (row < firstColumn.length - 1)
							firstColContents = firstColumn[row + 1]
									.getContents();
						if (bw.isWritableProperty(firstColContents)
								|| firstColContents.trim().equals("")
								|| row == firstColumn.length - 1) {
							bw.setPropertyValue(propertyName, map);
							if (log.isTraceEnabled())
								log.trace(" set map " + propertyName
										+ " of size " + map.size());
							break mapRows;// map is over
						}
					}
				} else {
					loadCell(cell, bw, propertyName, keyProperty, row,
							references);
				}

			}
			saveOrUpdate(bw.getPropertyValue(keyProperty), bw
					.getWrappedInstance(), clss);
			// tempRefs.get(sheet.getName()).add(bw.getWrappedInstance());
			registerInTabularView(sheet.getName(), bw.getWrappedInstance());
		}// columns
	}

	protected void loadCell(Cell cell, BeanWrapper bw, String propertyName,
			String keyProperty, Integer row, List<Reference> references)
			throws JXLException {

		if (cell instanceof FormulaCell) {
			String formula = ((FormulaCell) cell).getFormula();
			int index = formula.indexOf('!');
			if (index < 0)
				throw new ArgeoServerException("Cannot interpret formula "
						+ formula);
			;
			String targetSheet = formula.substring(0, index);
			// assume no double letters!!
			String targetRowStr = formula.substring(index + 2);
			if (targetRowStr.charAt(0) == '$')
				targetRowStr = targetRowStr.substring(1);
			Integer targetRow = Integer.parseInt(targetRowStr);
			references.add(new TabularInternalReference(
					bw.getWrappedInstance(), propertyName, targetSheet,
					targetRow));

			if (log.isTraceEnabled())
				log.debug("  formula: " + formula + " | content: "
						+ cell.getContents() + " | targetSheet=" + targetSheet
						+ ", targetRow=" + targetRow);
		} else {
			String contents = cell.getContents();

			// if (cell.getType() == CellType.LABEL) {
			// LabelCell lc = (LabelCell) cell;
			// contents = lc.getString();
			// } else if (cell.getType() == CellType.NUMBER) {
			// NumberCell nc = (NumberCell) cell;
			// contents = new Double(nc.getValue()).toString();
			// } else {
			// contents = cell.getContents();
			// }

			if (propertyName.equals(keyProperty)
					&& !StringUtils.hasText(contents)) {
				// auto allocate key column if empty
				contents = Integer.toString(row);
			}

			if (propertyName.charAt(0) == '#') {// externalRef
				references.add(new Reference(bw.getWrappedInstance(),
						propertyName.substring(1), contents));
			} else {
				bw.setPropertyValue(propertyName, contents);
			}

			if (log.isTraceEnabled())
				log.debug("  " + propertyName + "=" + contents);
		}

	}

	/** Returns true if property was set (thus bypassing standard process). */
	protected Boolean overrideCell(Cell cell, BeanWrapper bw,
			String propertyName, String keyProperty, Integer row,
			List<Reference> references) {
		return false;
	}

	/**
	 * @deprecated use
	 *             {@link #overrideCell(Cell, BeanWrapper, String, String, Integer, List)}
	 *             instead. This method is not called anymore.
	 */
	protected Boolean overrideCell(Cell cell, BeanWrapper bw,
			String propertyName, String keyProperty, Integer row,
			List<Reference> references, Map<String, List<Object>> tempRefs) {
		throw new UnsupportedOperationException();
	}

	protected Class<?> findClassToInstantiate(Sheet sheet) {
		// TODO: ability to map sheet names and class names
		String className = sheet.getName();
		Class<?> clss = null;
		try {
			clss = getClassLoader().loadClass(className);
			return clss;
		} catch (ClassNotFoundException e) {
			// silent
		}

		scannedPkgs: for (String pkg : getScannedPackages()) {
			try {
				clss = getClassLoader().loadClass(pkg.trim() + "." + className);
				break scannedPkgs;
			} catch (ClassNotFoundException e) {
				// silent
				if (log.isTraceEnabled())
					log.trace(e.getMessage());
			}
		}

		if (clss == null)
			throw new ArgeoServerException("Cannot find a class for sheet "
					+ sheet.getName());

		return clss;
	}

	public void setCharset(Integer charset) {
		this.charset = charset;
	}
}
