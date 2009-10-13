package org.argeo.server.jxl.dao;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jxl.Cell;
import jxl.FormulaCell;
import jxl.JXLException;
import jxl.Sheet;
import jxl.Workbook;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.ArgeoServerException;
import org.argeo.server.dao.LightDaoSupport;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.generic.GenericBeanFactoryAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

public class JxlDaoSupport implements LightDaoSupport, ApplicationContextAware,
		InitializingBean {
	private final static Log log = LogFactory.getLog(JxlDaoSupport.class);

	private ClassLoader classLoader = getClass().getClassLoader();
	private ApplicationContext applicationContext;
	private List<Class<?>> additionalClasses = new ArrayList<Class<?>>();

	private Map<Class<?>, Map<Object, Object>> model = new HashMap<Class<?>, Map<Object, Object>>();

	private Map<String, Object> externalRefs = new HashMap<String, Object>();

	private List<String> scannedPackages = new ArrayList<String>();

	private List<Resource> workbooks = new ArrayList<Resource>();

	public void afterPropertiesSet() throws Exception {
		init();
	}

	public void init() {
		// used to resolve inner references
		Map<String, List<Object>> tempRefs = new HashMap<String, List<Object>>();

		List<Reference> references = new ArrayList<Reference>();

		for (Resource res : workbooks) {
			InputStream in = null;
			try {
				in = res.getInputStream();
				load(in, references, tempRefs);
			} catch (Exception e) {
				throw new ArgeoServerException("Cannot load stream", e);
			} finally {
				IOUtils.closeQuietly(in);
			}
		}

		// Inject references
		for (Reference ref : references) {
			injectReference(ref, tempRefs);
		}
		if (log.isDebugEnabled())
			log.debug(references.size() + " references linked");
	}

	public List<Class<?>> getSupportedClasses() {
		List<Class<?>> res = new ArrayList<Class<?>>();
		res.addAll(additionalClasses);
		res.addAll(model.keySet());
		return res;
	}

	public void load(InputStream in, List<Reference> references,
			Map<String, List<Object>> tempRefs) {
		try {
			Workbook workbook = Workbook.getWorkbook(in);
			for (Sheet sheet : workbook.getSheets()) {
				loadSheet(sheet, references, tempRefs);
			}
		} catch (Exception e) {
			throw new ArgeoServerException("Cannot load workbook", e);
		}
	}

	protected void loadSheet(Sheet sheet, List<Reference> references,
			Map<String, List<Object>> tempRefs) throws JXLException {
		if (log.isTraceEnabled())
			log.debug("Instantiate sheet " + sheet.getName());

		Cell[] firstRow = sheet.getRow(0);

		Class<?> clss = findClassToInstantiate(sheet);
		model.put(clss, new TreeMap<Object, Object>());

		tempRefs.put(sheet.getName(), new ArrayList<Object>());

		String keyProperty = firstRow[0].getContents();
		for (int row = 1; row < sheet.getRows(); row++) {
			if (log.isTraceEnabled())
				log.trace(" row " + row);

			Cell[] currentRow = sheet.getRow(row);
			BeanWrapper bw = new BeanWrapperImpl(clss);
			cells: for (int col = 0; col < firstRow.length; col++) {
				String pName = firstRow[col].getContents();

				if (col < currentRow.length) {
					Cell cell = currentRow[col];
					if (overrideCell(cell, bw, pName, keyProperty, row,
							references, tempRefs))
						continue cells;
					loadCell(cell, bw, pName, keyProperty, row, references);
				}
			}// cells

			model.get(clss).put(bw.getPropertyValue(keyProperty),
					bw.getWrappedInstance());
			tempRefs.get(sheet.getName()).add(bw.getWrappedInstance());
		}

		if (log.isDebugEnabled())
			log.debug(model.get(clss).size() + " objects of type " + clss
					+ " instantiated");

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
			references.add(new Reference(bw.getWrappedInstance(), propertyName,
					targetSheet, targetRow));

			if (log.isTraceEnabled())
				log.debug("  formula: " + formula + " | content: "
						+ cell.getContents() + " | targetSheet=" + targetSheet
						+ ", targetRow=" + targetRow);
		} else {
			String contents = cell.getContents();
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
			List<Reference> references, Map<String, List<Object>> tempRefs) {
		return false;
	}

	protected void injectReference(Reference reference,
			Map<String, List<Object>> tempRefs) {
		BeanWrapper bw = new BeanWrapperImpl(reference.object);
		Object targetObject;
		if (reference.getExternalRef() != null) {
			String ref = reference.getExternalRef();
			if (externalRefs.containsKey(ref))
				targetObject = externalRefs.get(ref);
			else if (applicationContext != null)
				targetObject = applicationContext.getBean(ref);
			else {
				targetObject = null;
				log.warn("Ref " + ref + " not found");
			}
		} else {
			targetObject = tempRefs.get(reference.getTargetSheet()).get(
					reference.targetRow - 2);
		}
		bw.setPropertyValue(reference.property, targetObject);

	}

	protected Class<?> findClassToInstantiate(Sheet sheet) {
		// TODO: ability to map sheet names and class names
		String className = sheet.getName();
		Class<?> clss = null;
		try {
			clss = classLoader.loadClass(className);
			return clss;
		} catch (ClassNotFoundException e) {
			// silent
		}

		scannedPkgs: for (String pkg : scannedPackages) {
			try {
				clss = classLoader.loadClass(pkg.trim() + "." + className);
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

	@SuppressWarnings("unchecked")
	public <T> T getByKey(Class<T> clss, Object key) {
		return (T) model.get(findClass(clss)).get(key);
	}

	/**
	 * Slow.
	 * 
	 * @return the first found
	 */
	public <T> T getByField(Class<T> clss, String field, Object value) {
		List<T> all = list(clss, null);
		T res = null;
		for (T obj : all) {
			if (new BeanWrapperImpl(obj).getPropertyValue(field).equals(value)) {
				res = obj;
				break;
			}
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> list(Class<T> clss, Object filter) {
		List<T> res = new ArrayList<T>();

		Class classToUse = findClass(clss);
		if (classToUse != null)
			res.addAll((Collection<T>) model.get(classToUse).values());

		if (applicationContext != null)
			res.addAll(new GenericBeanFactoryAccessor(applicationContext)
					.getBeansOfType(clss).values());

		return res;
	}

	@SuppressWarnings("unchecked")
	protected Class findClass(Class parent) {
		if (model.containsKey(parent))
			return parent;

		for (Class clss : model.keySet()) {
			if (parent.isAssignableFrom(clss))
				return clss;// return the first found
		}
		return null;
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

	public void setScannedPackages(List<String> scannedPackages) {
		this.scannedPackages = scannedPackages;
	}

	public List<String> getScannedPackages() {
		return scannedPackages;
	}

	public void setWorkbooks(List<Resource> workbooks) {
		this.workbooks = workbooks;
	}

	public List<Resource> getWorkbooks() {
		return workbooks;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
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
