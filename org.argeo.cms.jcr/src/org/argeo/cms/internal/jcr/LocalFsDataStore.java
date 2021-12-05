package org.argeo.cms.internal.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.data.FileDataStore;

/**
 * <b>experimental</b> Duplicate added entries in another directory (typically a
 * remote mount).
 */
@SuppressWarnings("restriction")
public class LocalFsDataStore extends FileDataStore {
	String redundantPath;
	FileDataStore redundantStore;

	@Override
	public void init(String homeDir) {
		// init primary first
		super.init(homeDir);

		if (redundantPath != null) {
			// redundant directory must be created first
			// TODO implement some polling?
			if (Files.exists(Paths.get(redundantPath))) {
				redundantStore = new FileDataStore();
				redundantStore.setPath(redundantPath);
				redundantStore.init(homeDir);
			}
		}
	}

	@Override
	public DataRecord addRecord(InputStream input) throws DataStoreException {
		DataRecord dataRecord = super.addRecord(input);
		syncRedundantRecord(dataRecord);
		return dataRecord;
	}

	@Override
	public DataRecord getRecord(DataIdentifier identifier) throws DataStoreException {
		DataRecord dataRecord = super.getRecord(identifier);
		syncRedundantRecord(dataRecord);
		return dataRecord;
	}

	protected void syncRedundantRecord(DataRecord dataRecord) throws DataStoreException {
		if (redundantStore == null)
			return;
		if (redundantStore.getRecordIfStored(dataRecord.getIdentifier()) == null) {
			try (InputStream redundant = dataRecord.getStream()) {
				redundantStore.addRecord(redundant);
			} catch (IOException e) {
				throw new DataStoreException("Cannot add redundant record.", e);
			}
		}
	}

	public void setRedundantPath(String redundantPath) {
		this.redundantPath = redundantPath;
	}

}
