package org.argeo.cms.file.provider;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.List;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.DName;

public class ContentFileAttributeView implements BasicFileAttributeView, UserDefinedFileAttributeView {
	final static String NAME = "content";

	private final Content content;

	public ContentFileAttributeView(Content content) {
		this.content = content;
	}

	@Override
	public String name() {
		return NAME;
	}

	/*
	 * BasicFileAttributeView
	 */

	@Override
	public BasicFileAttributes readAttributes() throws IOException {
		return new ContentAttributes(content);
	}

	@Override
	public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
		if (lastModifiedTime != null)
			content.put(DName.getlastmodified, lastModifiedTime.toInstant());
		if (createTime != null)
			content.put(DName.getlastmodified, createTime.toInstant());
		// ignore last accessed time
	}

	/*
	 * UserDefinedFileAttributeView
	 */

	@Override
	public List<String> list() throws IOException {
//		List<String> res = new ArrayList<>();
		return null;
	}

	@Override
	public int size(String name) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int read(String name, ByteBuffer dst) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int write(String name, ByteBuffer src) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void delete(String name) throws IOException {
		// TODO Auto-generated method stub

	}

	public Content getContent() {
		return content;
	}
}
