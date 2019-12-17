package org.argeo.osgi.a2;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.SortedSet;
import java.util.TreeSet;

import org.argeo.osgi.boot.OsgiBootUtils;
import org.osgi.framework.Version;

/** A file system {@link AbstractProvisioningSource} in A2 format. */
public class FsA2Source extends AbstractProvisioningSource implements A2Source {
	private final Path base;

	public FsA2Source(Path base) {
		super();
		this.base = base;
	}

	void load() throws IOException {
		DirectoryStream<Path> contributionPaths = Files.newDirectoryStream(base);
		SortedSet<A2Contribution> contributions = new TreeSet<>();
		contributions: for (Path contributionPath : contributionPaths) {
			if (Files.isDirectory(contributionPath)) {
				String contributionId = contributionPath.getFileName().toString();
				if (A2Contribution.BOOT.equals(contributionId))// skip boot
					continue contributions;
				A2Contribution contribution = getOrAddContribution(contributionId);
				contributions.add(contribution);
			}
		}

		for (A2Contribution contribution : contributions) {
			DirectoryStream<Path> modulePaths = Files.newDirectoryStream(base.resolve(contribution.getId()));
			modules: for (Path modulePath : modulePaths) {
				if (!Files.isDirectory(modulePath)) {
					// OsgiBootUtils.debug("Registering " + modulePath);
					String moduleFileName = modulePath.getFileName().toString();
					int lastDot = moduleFileName.lastIndexOf('.');
					String ext = moduleFileName.substring(lastDot + 1);
					if (!"jar".equals(ext))
						continue modules;
					String moduleName = moduleFileName.substring(0, lastDot);
					if (moduleName.endsWith("-SNAPSHOT"))
						moduleName = moduleName.substring(0, moduleName.length() - "-SNAPSHOT".length());
					int lastDash = moduleName.lastIndexOf('-');
					String versionStr = moduleName.substring(lastDash + 1);
					String componentName = moduleName.substring(0, lastDash);
					// if(versionStr.endsWith("-SNAPSHOT")) {
					// versionStr = readVersionFromModule(modulePath);
					// }
					Version version;
					try {
						version = new Version(versionStr);
					} catch (Exception e) {
						versionStr = readVersionFromModule(modulePath);
						if (versionStr != null) {
							version = new Version(versionStr);
						} else {
							OsgiBootUtils.debug("Ignore " + modulePath + " (" + e.getMessage() + ")");
							continue modules;
						}
					}
					A2Component component = contribution.getOrAddComponent(componentName);
					A2Module module = component.getOrAddModule(version, modulePath);
					if (OsgiBootUtils.isDebug())
						OsgiBootUtils.debug("Registered " + module);
				}
			}
		}

	}

	public static void main(String[] args) {
		try {
			FsA2Source context = new FsA2Source(Paths.get(
					"/home/mbaudier/dev/git/apache2/argeo-commons/dist/argeo-node/target/argeo-node-2.1.77-SNAPSHOT/share/osgi"));
			context.load();
			context.asTree();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
