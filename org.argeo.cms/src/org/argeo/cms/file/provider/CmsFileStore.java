package org.argeo.cms.file.provider;

import java.io.IOException;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

import org.argeo.api.acr.fs.AbstractFsStore;
import org.argeo.api.acr.spi.ContentProvider;

public class CmsFileStore extends AbstractFsStore {
	private final ContentProvider contentProvider;

	public CmsFileStore(ContentProvider contentProvider) {
		this.contentProvider = contentProvider;
	}

	@Override
	public String name() {
		// TODO return an URI
		String name = contentProvider.getMountPath();
		return name;
	}

	@Override
	public String type() {
		String type = contentProvider.getClass().getName();
		return type;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public long getTotalSpace() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getUsableSpace() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getUnallocatedSpace() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		if (ContentAttributeView.class.isAssignableFrom(type))
			return true;
		return false;
	}

	@Override
	public boolean supportsFileAttributeView(String name) {
		if (ContentAttributeView.NAME.equals(name))
			return true;
		return false;
	}

	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAttribute(String attribute) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
