package org.argeo.eclipse.ui.fs;

import java.nio.file.Path;

/** A parent directory (..) reference. */
public class ParentDir {
	Path path;

	public ParentDir(Path path) {
		super();
		this.path = path;
	}

	public Path getPath() {
		return path;
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	@Override
	public String toString() {
		return "Parent dir " + path;
	}

}
