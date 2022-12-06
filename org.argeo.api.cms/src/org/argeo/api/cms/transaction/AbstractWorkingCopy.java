package org.argeo.api.cms.transaction;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractWorkingCopy<DATA, ATTR, ID> implements WorkingCopy<DATA, ATTR, ID> {
	private Map<ID, DATA> newData = new HashMap<ID, DATA>();
	private Map<ID, ATTR> modifiedData = new HashMap<ID, ATTR>();
	private Map<ID, DATA> deletedData = new HashMap<ID, DATA>();

	protected abstract ID getId(DATA data);

	protected abstract ATTR cloneAttributes(DATA data);

	public void cleanUp() {
		// clean collections
		newData.clear();
		newData = null;
		modifiedData.clear();
		modifiedData = null;
		deletedData.clear();
		deletedData = null;
	}

	public boolean noModifications() {
		return newData.size() == 0 && modifiedData.size() == 0 && deletedData.size() == 0;
	}

	public void startEditing(DATA user) {
		ID id = getId(user);
		if (modifiedData.containsKey(id))
			throw new IllegalStateException("Already editing " + id);
		modifiedData.put(id, cloneAttributes(user));
	}

	public Map<ID, DATA> getNewData() {
		return newData;
	}

	public Map<ID, DATA> getDeletedData() {
		return deletedData;
	}

	public Map<ID, ATTR> getModifiedData() {
		return modifiedData;
	}

}
