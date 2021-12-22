package org.argeo.api.gcr.fs;

import java.nio.file.FileSystem;

public abstract class AbstractFsSystem<ST extends AbstractFsStore> extends FileSystem {
	public abstract ST getBaseFileStore();

	public abstract ST getFileStore(String path);
}
