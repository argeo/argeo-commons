package org.argeo.server.jxl.dao;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import jxl.Cell;
import jxl.CellType;
import jxl.FormulaCell;
import jxl.JXLException;
import jxl.LabelCell;
import jxl.NumberCell;
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
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

public class JxlDaoSupport extends AbstractTabularDaoSupport implements
		LightDaoSupport, ApplicationContextAware, InitializingBean {
	private final static Log log = LogFactory.getLog(JxlDaoSupport.class);

	private String encoding = "cp1252";
	private Locale locale = null;

	protected void load(InputStream in, List<Reference> references) {
		try {
			WorkbookSettings workbookSettings = new WorkbookSettings();
			workbookSettings.setEncoding(encoding);
			if (locale != null)
				workbookSettings.setLocale(locale);
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
		String sheetName = sheet.getName();
		if (log.isTraceEnabled())
			log.debug("Instantiate sheet " + sheetName);

		String tableName;
		int hashIndex = sheetName.lastIndexOf('#');
		if (hashIndex >= 0) {
			tableName = sheetName.substring(0, hashIndex);
		} else {
			tableName = sheetName;
		}

		Class<?> clss = findClassToInstantiate(tableName);

		if (hashIndex >= 0) {
			// see
			// http://stackoverflow.com/questions/451452/valid-characters-for-excel-sheet-names
			BeanWrapper bw = newBeanWrapper(clss);
			StringTokenizer espSt = new StringTokenizer(sheetName
					.substring(hashIndex + 1), "&=");
			String keyProperty = null;
			while (espSt.hasMoreTokens()) {
				String fieldName = espSt.nextToken();
				if (keyProperty == null)
					keyProperty = fieldName;
				if (!espSt.hasMoreTokens())
					throw new ArgeoServerException("Badly formatted sheetname "
							+ sheetName);
				String fieldValue = espSt.nextToken();
				bw.setPropertyValue(fieldName, fieldValue);
				loadAsObject(bw, sheet, references);
				saveOrUpdate(bw.getPropertyValue(keyProperty), bw
						.getWrappedInstance(), clss);
			}

		} else {

			Cell[] firstRow = sheet.getRow(0);
			String keyProperty = firstRow[0].getContents();

			if (keyProperty.charAt(keyProperty.length() - 1) == '>') {
				loadAsColumns(clss, keyProperty.substring(0, keyProperty
						.length() - 1), sheet, firstRow, references);
			} else {
				loadAsRows(clss, keyProperty, sheet, firstRow, references);
			}
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
						if (log.isTraceEnabled())
							log.trace("   row=" + row + ", firstColContents="
									+ firstColContents + ", key=" + key
									+ ", type=" + cell.getType());
						Object cellValue = getCellValue(cell);
						map.put(key, cellValue);

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

	protected void loadAsObject(BeanWrapper bw, Sheet sheet,
			List<Reference> references) {
		Cell[] firstColumn = sheet.getColumn(0);
		for (int row = 0; row < firstColumn.length; row++) {
			if (log.isTraceEnabled())
				log.trace(" row " + row);
			Cell[] currentRow = sheet.getRow(row);
			String propertyName = firstColumn[row].getContents();
			Class<?> rowType = bw.getPropertyType(propertyName);
			if (Map.class.isAssignableFrom(rowType)) {
				Map<Object, Object> map = new HashMap<Object, Object>();
				if (currentRow.length == 1
						|| currentRow[1].getContents().trim().equals("")) {
					// simple map
				} else {
					// map of maps
					List<Object> subKeys = new ArrayList<Object>();
					for (int col = 1; col < currentRow.length; col++) {
						subKeys.add(getCellValue(currentRow[col]));
					}
					if (log.isTraceEnabled())
						log.trace("   subKeys=" + subKeys);
					row++;
					String firstColContents = firstColumn[row].getContents();
					mapRows: for (; row < firstColumn.length; row++) {
						currentRow = sheet.getRow(row);

						Object key = firstColContents;
						Map<Object, Object> subMap = new HashMap<Object, Object>();

						for (int col = 1; col < currentRow.length
								&& col < subKeys.size() + 1; col++) {
							Object subKey = subKeys.get(col - 1);
							Cell cell = currentRow[col];
							if (log.isTraceEnabled())
								log.trace("   row=" + row
										+ ", firstColContents="
										+ firstColContents + ", subKey="
										+ subKey + ", type=" + cell.getType());
							Object cellValue = getCellValue(cell);
							subMap.put(subKey, cellValue);
						}
						map.put(key, subMap);

						// check next row too see if one should break
						if (row < firstColumn.length - 1)
							firstColContents = firstColumn[row + 1]
									.getContents();
						if (bw.isWritableProperty(firstColContents)
								|| firstColContents.trim().equals("")
								|| row == firstColumn.length - 1) {
							log.trace(map);
							bw.setPropertyValue(propertyName, map);
							if (log.isTraceEnabled())
								log.trace(" set map " + propertyName
										+ " of size " + map.size());
							break mapRows;// map is over
						}
					}

				}
			} else if (List.class.isAssignableFrom(rowType)) {
				throw new UnsupportedOperationException();
			} else {
				bw.setPropertyValue(propertyName, getCellValue(currentRow[1]));
			}
		}
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
			Object cellValue = getCellValue(cell);

			if (propertyName.equals(keyProperty)
					&& !StringUtils.hasText(cellValue.toString())) {
				// auto allocate key column if empty
				cellValue = Integer.toString(row);
			}

			if (propertyName.charAt(0) == '#') {// externalRef
				references.add(new Reference(bw.getWrappedInstance(),
						propertyName.substring(1), cellValue.toString()));
			} else {
				bw.setPropertyValue(propertyName, cellValue);
			}

			if (log.isTraceEnabled())
				log.debug("  " + propertyName + "=" + cellValue);
		}

	}

	protected Object getCellValue(Cell cell) {
		Object contents;
		if (cell.getType() == CellType.LABEL) {
			LabelCell lc = (LabelCell) cell;
			contents = lc.getString();
		} else if (cell.getType() == CellType.NUMBER) {
			NumberCell nc = (NumberCell) cell;
			contents = nc.getValue();
		} else {
			contents = cell.getContents();
		}
		return contents;
	}

	/** Returns true if property was set (thus bypassing standard process). */
	protected Boolean overrideCell(Cell cell, BeanWrapper bw,
			String propertyName, String keyProperty, Integer row,
			List<Reference> references) {
		return false;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

}
