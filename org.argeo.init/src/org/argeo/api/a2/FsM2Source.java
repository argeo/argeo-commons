package org.argeo.api.a2;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.osgi.framework.Version;

/** A file system {@link AbstractProvisioningSource} in Maven 2 format. */
public class FsM2Source extends AbstractProvisioningSource {
	private final static Logger logger = System.getLogger(FsM2Source.class.getName());

	private final Path base;

	public FsM2Source(Path base) {
		super(false);
		this.base = base;
	}

	void load() throws IOException {
		Files.walkFileTree(base, new ArtifactFileVisitor());
	}

	class ArtifactFileVisitor extends SimpleFileVisitor<Path> {

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			// OsgiBootUtils.debug("Processing " + file);
			if (file.toString().endsWith(".jar")) {
				Version version;
				try {
					version = new Version(readVersionFromModule(file));
				} catch (Exception e) {
					// ignore non OSGi
					return FileVisitResult.CONTINUE;
				}
				String moduleName = readSymbolicNameFromModule(file);
				Path groupPath = file.getParent().getParent().getParent();
				Path relGroupPath = base.relativize(groupPath);
				String contributionName = relGroupPath.toString().replace(File.separatorChar, '.');
				A2Contribution contribution = getOrAddContribution(contributionName);
				A2Component component = contribution.getOrAddComponent(moduleName);
				A2Module module = component.getOrAddModule(version, file);
				logger.log(Level.TRACE, () -> "Registered " + module);
			}
			return super.visitFile(file, attrs);
		}

	}

	public static void main(String[] args) {
		try {
			FsM2Source context = new FsM2Source(Paths.get("/home/mbaudier/.m2/repository"));
			context.load();
			context.asTree();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
