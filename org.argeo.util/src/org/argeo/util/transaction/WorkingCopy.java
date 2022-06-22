package org.argeo.util.transaction;

import java.util.Map;

public interface WorkingCopy<DATA, ATTR, ID> {
	void startEditing(DATA user);

	boolean noModifications();

	void cleanUp();

	Map<ID, DATA> getNewData();

	Map<ID, DATA> getDeletedData();

	Map<ID, ATTR> getModifiedData();

}
