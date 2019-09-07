package org.argeo.fs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** {@link FsUtils} tests. */
public class FsUtilsTest {
	final static String FILE00 = "file00";
	final static String FILE01 = "file01";
	final static String SUB_DIR = "subDir";

	public void testDelete() throws IOException {
		Path dir = createDir00();
		assert Files.exists(dir);
		FsUtils.delete(dir);
		assert !Files.exists(dir);
	}

	public void testSync() throws IOException {
		Path source = createDir00();
		Path target = Files.createTempDirectory(getClass().getName());
		FsUtils.sync(source, target);
		assert Files.exists(target.resolve(FILE00));
		assert Files.exists(target.resolve(SUB_DIR));
		assert Files.exists(target.resolve(SUB_DIR + File.separator + FILE01));
		FsUtils.delete(source.resolve(SUB_DIR));
		FsUtils.sync(source, target, true);
		assert Files.exists(target.resolve(FILE00));
		assert !Files.exists(target.resolve(SUB_DIR));
		assert !Files.exists(target.resolve(SUB_DIR + File.separator + FILE01));

		// clean up
		FsUtils.delete(source);
		FsUtils.delete(target);

	}

	Path createDir00() throws IOException {
		Path base = Files.createTempDirectory(getClass().getName());
		base.toFile().deleteOnExit();
		Files.createFile(base.resolve(FILE00)).toFile().deleteOnExit();
		Path subDir = Files.createDirectories(base.resolve(SUB_DIR));
		subDir.toFile().deleteOnExit();
		Files.createFile(subDir.resolve(FILE01)).toFile().deleteOnExit();
		return base;
	}
}
