package org.argeo.server.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.ArgeoServerException;

public abstract class AbstractTabularDaoSupport extends
		AbstractMemoryDaoSupport {
	private final static Log log = LogFactory
			.getLog(AbstractTabularDaoSupport.class);
	
	private Map<String, List<Object>> tabularView = new HashMap<String, List<Object>>();

	@Override
	protected Object findInternalRef(Reference reference) {
		TabularInternalReference tabReference = (TabularInternalReference) reference;
		return getFromTabularView(tabReference.getTargetTableName(),
				tabReference.getTargetRow());
	}

	protected Object getFromTabularView(String tableName, Integer row) {
		return tabularView.get(tableName).get(row - 2);
	}

	protected void registerInTabularView(String tableName, Object object) {
		if (!tabularView.containsKey(tableName))
			tabularView.put(tableName, new ArrayList<Object>());
		tabularView.get(tableName).add(object);
	}

	protected Class<?> findClassToInstantiate(String tableName) {
		// TODO: ability to map sheet names and class names
		String className = tableName;
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
			throw new ArgeoServerException("Cannot find a class for table "
					+ tableName);

		return clss;
	}

	protected static class TabularInternalReference extends Reference {
		private String targetTableName;
		private Integer targetRow;

		public TabularInternalReference(Object object, String property,
				String targetSheet, Integer targetRow) {
			super(object, property, null);
			this.targetTableName = targetSheet;
			this.targetRow = targetRow;
		}

		public String getTargetTableName() {
			return targetTableName;
		}

		public Integer getTargetRow() {
			return targetRow;
		}

	}
}
