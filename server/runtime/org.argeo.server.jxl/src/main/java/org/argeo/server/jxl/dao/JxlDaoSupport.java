package org.argeo.server.jxl.dao;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
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
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

public class JxlDaoSupport implements ApplicationContextAware {
	private final static Log log = LogFactory.getLog(JxlDaoSupport.class);

	private ClassLoader classLoader = getClass().getClassLoader();
	private ApplicationContext applicationContext;

	private Map<Class<?>, Map<Object, Object>> model = new HashMap<Class<?>, Map<Object, Object>>();

	private Map<String, Object> externalRefs = new HashMap<String, Object>();

	public void load(InputStream in) {

		try {
			// used to resolve inner references
			Map<String, List<Object>> tempRefs = new HashMap<String, List<Object>>();
			List<Reference> links = new ArrayList<Reference>();

			Workbook workbook = Workbook.getWorkbook(in);

			for (Sheet sheet : workbook.getSheets()) {
				if (log.isTraceEnabled())
					log.debug("Instantiate sheet " + sheet.getName());

				Cell[] firstRow = sheet.getRow(0);

				// TODO: ability to map sheet names and class names
				String className = sheet.getName();
				Class<?> clss = classLoader.loadClass(className);
				model.put(clss, new HashMap<Object, Object>());

				tempRefs.put(sheet.getName(), new ArrayList<Object>());

				String keyProperty = firstRow[0].getContents();
				for (int row = 1; row < sheet.getRows(); row++) {
					if (log.isTraceEnabled())
						log.trace(" row " + row);

					Cell[] currentRow = sheet.getRow(row);
					BeanWrapper bw = new BeanWrapperImpl(clss);
					for (int col = 0; col < firstRow.length; col++) {
						String pName = firstRow[col].getContents();

						Cell cell = currentRow[col];
						if (cell instanceof FormulaCell) {
							String formula = ((FormulaCell) cell).getFormula();
							int index = formula.indexOf('!');
							String targetSheet = formula.substring(0, index);
							// assume no double letters!!
							String targetRowStr = formula.substring(index + 2);
							if (targetRowStr.charAt(0) == '$')
								targetRowStr = targetRowStr.substring(1);
							Integer targetRow = Integer.parseInt(targetRowStr);
							links.add(new Reference(bw.getWrappedInstance(),
									pName, targetSheet, targetRow));

							if (log.isTraceEnabled())
								log.debug("  formula: " + formula
										+ " | content: " + cell.getContents()
										+ " | targetSheet=" + targetSheet
										+ ", targetRow=" + targetRow);
						} else {
							String contents = cell.getContents();
							if (pName.equals(keyProperty)
									&& !StringUtils.hasText(contents)) {
								// auto allocate key column if empty
								contents = Integer.toString(row);
							}

							if (pName.charAt(0) == '#') {// externalRef
								links.add(new Reference(
										bw.getWrappedInstance(), pName
												.substring(1), contents));
							} else {
								bw.setPropertyValue(pName, contents);
							}

							if (log.isTraceEnabled())
								log.debug("  " + pName + "=" + contents);
						}
					}// properties set

					model.get(clss).put(bw.getPropertyValue(keyProperty),
							bw.getWrappedInstance());
					tempRefs.get(sheet.getName()).add(bw.getWrappedInstance());
				}

				if (log.isDebugEnabled())
					log.debug(model.get(clss).size() + " objects of type "
							+ clss + " instantiated");
			}

			for (Reference link : links) {
				BeanWrapper bw = new BeanWrapperImpl(link.object);
				Object targetObject;
				if (link.getExternalRef() != null) {
					String ref = link.getExternalRef();
					if (externalRefs.containsKey(ref))
						targetObject = externalRefs.get(ref);
					else if (applicationContext != null)
						targetObject = applicationContext.getBean(ref);
					else {
						targetObject = null;
						log.warn("Ref " + ref + " not found");
					}
				} else {
					targetObject = tempRefs.get(link.getTargetSheet()).get(
							link.targetRow - 2);
				}
				bw.setPropertyValue(link.property, targetObject);
			}
			if (log.isDebugEnabled())
				log.debug(links.size() + " references linked");

		} catch (Exception e) {
			throw new ArgeoServerException("Cannot load workbook", e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getByKey(Class<T> clss, Object key) {
		return (T) model.get(findClass(clss)).get(key);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> list(Class<T> clss, Object filter) {
		return new ArrayList<T>((Collection<T>) model.get(findClass(clss))
				.values());
	}

	@SuppressWarnings("unchecked")
	protected Class findClass(Class parent) {
		if (model.containsKey(parent))
			return parent;

		for (Class clss : model.keySet()) {
			if (parent.isAssignableFrom(clss))
				return clss;// return the first found
		}
		throw new ArgeoServerException("No implementing class found for "
				+ parent);
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void setExternalRefs(Map<String, Object> externalRefs) {
		this.externalRefs = externalRefs;
	}

	public Map<String, Object> getExternalRefs() {
		return externalRefs;
	}

	public static class Reference {
		private Object object;
		private String property;
		private String targetSheet;
		private Integer targetRow;
		private String externalRef;

		public Reference(Object object, String property, String targetSheet,
				Integer targetRow) {
			this.object = object;
			this.property = property;
			this.targetSheet = targetSheet;
			this.targetRow = targetRow;
		}

		public Reference(Object object, String property, String externalRef) {
			this.object = object;
			this.property = property;
			this.externalRef = externalRef;
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

		public String getExternalRef() {
			return externalRef;
		}

	}
}
