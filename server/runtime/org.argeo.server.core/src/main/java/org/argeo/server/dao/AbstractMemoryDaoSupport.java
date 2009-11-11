package org.argeo.server.dao;

import java.beans.PropertyEditor;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.ArgeoServerException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.generic.GenericBeanFactoryAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

public abstract class AbstractMemoryDaoSupport implements LightDaoSupport,
		ApplicationContextAware, InitializingBean {
	private final static Log log = LogFactory
			.getLog(AbstractMemoryDaoSupport.class);

	private ClassLoader classLoader = getClass().getClassLoader();
	private ApplicationContext applicationContext;
	private List<Class<?>> additionalClasses = new ArrayList<Class<?>>();

	private Map<Class<?>, Map<Object, Object>> model = new HashMap<Class<?>, Map<Object, Object>>();

	private Map<String, Object> externalRefs = new HashMap<String, Object>();

	private List<String> scannedPackages = new ArrayList<String>();

	private List<Resource> resources = new ArrayList<Resource>();

	private Map<Class<?>, PropertyEditor> customEditors = new HashMap<Class<?>, PropertyEditor>();;

	protected abstract void load(InputStream in, List<Reference> references);

	protected abstract Object findInternalRef(Reference reference);

	public void afterPropertiesSet() throws Exception {
		init();
	}

	public void init() {
		for (PropertyEditor propertyEditor : customEditors.values())
			if (propertyEditor instanceof LightDaoAware) {
				((LightDaoAware) propertyEditor).setLightDaoSupport(this);
			}

		// Load data
		List<Reference> references = new ArrayList<Reference>();

		for (Resource res : resources) {
			InputStream in = null;
			try {
				in = res.getInputStream();
				load(in, references);
			} catch (Exception e) {
				throw new ArgeoServerException("Cannot load stream", e);
			} finally {
				IOUtils.closeQuietly(in);
			}
		}

		// Inject references
		for (Reference ref : references) {
			injectReference(ref);
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

	protected void injectReference(Reference reference) {
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
			targetObject = findInternalRef(reference);
		}
		bw.setPropertyValue(reference.property, targetObject);

	}

	protected BeanWrapper newBeanWrapper(Class<?> targetClass) {
		BeanWrapperImpl bw = new BeanWrapperImpl(targetClass);
		for (Class<?> clss : customEditors.keySet())
			bw.registerCustomEditor(clss, customEditors.get(clss));
		return bw;
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
		if (log.isDebugEnabled())
			log.warn("No class found for " + parent.getName());
		return null;
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * When it should be stored under a different class (e.g. super class or
	 * interface)
	 */
	public void saveOrUpdate(Object key, Object value, Class<?> clss) {
		if (!model.containsKey(clss))
			model.put(clss, new TreeMap<Object, Object>());
		model.get(clss).put(key, value);
	}

	protected ClassLoader getClassLoader() {
		return classLoader;
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

	public void setResources(List<Resource> workbooks) {
		this.resources = workbooks;
	}

	public List<Resource> getResources() {
		return resources;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public List<Class<?>> getAdditionalClasses() {
		return additionalClasses;
	}

	public void setAdditionalClasses(List<Class<?>> additionalClasses) {
		this.additionalClasses = additionalClasses;
	}

	public void setCustomEditors(Map<Class<?>, PropertyEditor> propertyEditors) {
		this.customEditors = propertyEditors;
	}

	protected static class Reference {
		private Object object;
		private String property;
		private String externalRef;

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

		public String getExternalRef() {
			return externalRef;
		}

	}
}
