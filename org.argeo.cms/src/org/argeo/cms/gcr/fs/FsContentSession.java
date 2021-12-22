package org.argeo.cms.gcr.fs;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.argeo.api.gcr.Content;
import org.argeo.api.gcr.ContentSystemProvider;
import org.argeo.api.gcr.ContentUtils;

public class FsContentSession implements ContentSystemProvider {
	private final Path rootPath;

	public FsContentSession(Path rootPath) {
		super();
		this.rootPath = rootPath;
	}

	@Override
	public Content get() {
		return new FsContent(this, rootPath);
	}

	public static void main(String[] args) {
		Path path = Paths.get("/home/mbaudier/tmp");
		System.out.println(FileSystems.getDefault().supportedFileAttributeViews());
		FsContentSession contentSession = new FsContentSession(path);
		ContentUtils.traverse(contentSession.get(), (c, d) -> ContentUtils.print(c, System.out, d, true));

	}
}
