package org.argeo.cms.file.provider;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.DName;

public class ContentAttributes implements BasicFileAttributes {
	// TODO optimise for FS-based content
	private final Content content;

	public ContentAttributes(Content content) {
		assert content != null;
		this.content = content;
	}

	@Override
	public FileTime lastModifiedTime() {
		Instant t = content.get(DName.getlastmodified, Instant.class).orElseThrow();
		return FileTime.from(t);
	}

	@Override
	public FileTime lastAccessTime() {
		// TODO implement the concept in ACR ?
		return FileTime.fromMillis(0l);
	}

	@Override
	public FileTime creationTime() {
		Instant t = content.get(DName.getlastmodified, Instant.class).orElseThrow();
		return FileTime.from(t);
	}

	@Override
	public boolean isRegularFile() {
		return isRegularFile(content);
	}

	@Override
	public boolean isDirectory() {
		return isDirectory(content);
	}

	@Override
	public boolean isSymbolicLink() {
		// TODO supports links in ACR
		return false;
	}

	@Override
	public boolean isOther() {
		return !isDirectory() && !isRegularFile() && !isSymbolicLink();
	}

	@Override
	public long size() {
		long size = content.get(DName.getcontentlength, Long.class).orElse(-1l);
		return size;
	}

	@Override
	public Object fileKey() {
		// TODO check for UUIDs, etc.
		return null;
	}

	static boolean isDirectory(Content c) {
		return !isRegularFile(c);
//		return c.isContentClass(DName.collection);
	}

	static boolean isRegularFile(Content c) {
//		return c.containsKey(DName.getcontenttype.qName());
		return !c.get(DName.getcontenttype, String.class).isEmpty();
	}

}
