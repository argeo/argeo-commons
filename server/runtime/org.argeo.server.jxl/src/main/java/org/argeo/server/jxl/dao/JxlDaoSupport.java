package org.argeo.server.jxl.dao;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jxl.Cell;
import jxl.FormulaCell;
import jxl.Sheet;
import jxl.Workbook;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.ArgeoServerException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public class JxlDaoSupport {
	private final static Log log = LogFactory.getLog(JxlDaoSupport.class);

	private ClassLoader classLoader = getClass().getClassLoader();

	private Map<Class, Map<Object, Object>> model = new HashMap<Class, Map<Object, Object>>();

	public void load(InputStream in) {

		try {
			// used to resolve inner references
			Map<String, List<Object>> tempRefs = new HashMap<String, List<Object>>();
			List<Link> links = new ArrayList<Link>();

			Workbook workbook = Workbook.getWorkbook(in);

			for (Sheet sheet : workbook.getSheets()) {
				if (log.isDebugEnabled())
					log
							.debug("Instantiate objects of sheet "
									+ sheet.getName());
				
				Cell[] firstRow = sheet.getRow(0);

				// TODO: ability to map sheet names and class names
				String className = sheet.getName();
				Class<?> clss = classLoader.loadClass(className);
				model.put(clss, new HashMap<Object, Object>());

				tempRefs.put(sheet.getName(), new ArrayList<Object>());

				String keyProperty = firstRow[0].getContents();
				for (int i = 1; i < sheet.getRows(); i++) {
					if (log.isTraceEnabled())
						log.trace(" row " + i);

					Cell[] currentRow = sheet.getRow(i);
					BeanWrapper bw = new BeanWrapperImpl(clss);
					for (int j = 0; j < firstRow.length; j++) {
						String pName = firstRow[j].getContents();

						Cell cell = currentRow[j];
						if (cell instanceof FormulaCell) {
							String formula = ((FormulaCell) cell).getFormula();
							int index = formula.indexOf('!');
							String targetSheet = formula.substring(0, index);
							// assume no double letters
							Integer targetRow = Integer.parseInt(formula
									.substring(index + 2));
							links.add(new Link(bw.getWrappedInstance(), pName,
									targetSheet, targetRow));

							if (log.isTraceEnabled())
								log.debug("  formula: " + formula
										+ " | content: " + cell.getContents()
										+ " | targetSheet=" + targetSheet
										+ ", targetRow=" + targetRow);
						} else {
							bw.setPropertyValue(pName, cell.getContents());

							if (log.isTraceEnabled())
								log.debug("  " + pName + "="
										+ cell.getContents());
						}
					}// properties set

					model.get(clss).put(bw.getPropertyValue(keyProperty),
							bw.getWrappedInstance());
					tempRefs.get(sheet.getName()).add(bw.getWrappedInstance());
				}

				if (log.isDebugEnabled())
					log.debug(model.get(clss).size() + " objects of type "
							+ clss);
			}

			if (log.isDebugEnabled())
				log.debug("Link " + links.size() + " references");
			for (Link link : links) {
				BeanWrapper bw = new BeanWrapperImpl(link.object);
				Object targetObject = tempRefs.get(link.getTargetSheet()).get(
						link.targetRow - 2);
				bw.setPropertyValue(link.property, targetObject);
			}

		} catch (Exception e) {
			throw new ArgeoServerException("Cannot load workbook", e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getByKey(Class<T> clss, Object key) {
		return (T) model.get(clss).get(key);
	}

	public static class Link {
		private Object object;
		private String property;
		private String targetSheet;
		private Integer targetRow;

		public Link(Object object, String property, String targetSheet,
				Integer targetRow) {
			super();
			this.object = object;
			this.property = property;
			this.targetSheet = targetSheet;
			this.targetRow = targetRow;
		}

		public Object getObject() {
			return object;
		}

		public String getProperty() {
			return property;
		}

		public String getTargetSheet() {
			return targetSheet;
		}

		public Integer getTargetRow() {
			return targetRow;
		}

	}
}
