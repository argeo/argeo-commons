package org.argeo.cms.file.provider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.DName;

public class ContentDirectoryStream implements DirectoryStream<Path> {
	private final CmsPath dir;
	private final Filter<? super Path> filter;

	private FilesAndCollectionsIterator iterator;

	public ContentDirectoryStream(CmsPath dir, Filter<? super Path> filter) {
		this.dir = dir;
		this.filter = filter;
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public Iterator<Path> iterator() {
		if (iterator == null)
			iterator = new FilesAndCollectionsIterator();
		return iterator;
	}

	static boolean isFile(Content c) {
		return !c.get(DName.getcontenttype, String.class).isEmpty();
	}

	static boolean isDirectory(Content c) {
		return c.isContentClass(DName.collection);
	}

	class FilesAndCollectionsIterator implements Iterator<Path> {
		private Content next;
		private final Iterator<Content> it;

		public FilesAndCollectionsIterator() {
			Content content = dir.getContent();
			if (!content.isContentClass(DName.collection))
				throw new IllegalStateException("Content " + content + " is not a collection");
			it = content.iterator();
			findNext();
		}

		private void findNext() {
			next = null;
			while (it.hasNext() && next != null) {
				Content n = it.next();
				if (isFile(n) || isDirectory(n)) {
					if (filter != null) {
						try {
							if (filter.accept(new CmsPath(dir.getFileSystem(), n)))
								next = n;
						} catch (IOException e) {
							throw new UncheckedIOException("Cannot filter " + dir, e);
						}
					} else {
						next = n;
					}
				}
			}
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public Path next() {
			if (next == null)
				throw new IllegalStateException("Iterator doesn't have more elements");
			CmsPath p = new CmsPath(dir.getFileSystem(), next);
			findNext();
			return p;
		}

	}
}
