package org.argeo.api.a2;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/** Where components are retrieved from. */
public abstract class AbstractProvisioningSource implements ProvisioningSource {
	protected final Map<String, A2Contribution> contributions = Collections.synchronizedSortedMap(new TreeMap<>());

	private final boolean usingReference;

	public AbstractProvisioningSource(boolean usingReference) {
		this.usingReference = usingReference;
	}

	public Iterable<A2Contribution> listContributions(Object filter) {
		return contributions.values();
	}

	@Override
	public Bundle install(BundleContext bc, A2Module module) {
		try {
			Object locator = module.getLocator();
			if (usingReference && locator instanceof Path locatorPath) {
				String referenceUrl = "reference:file:" + locatorPath.toString();
				Bundle bundle = bc.installBundle(referenceUrl);
				return bundle;
			} else {
				Path locatorPath = (Path) locator;
				Path pathToUse;
				boolean isTemp = false;
				if (locator instanceof Path && Files.isDirectory(locatorPath)) {
					pathToUse = toTempJar(locatorPath);
					isTemp = true;
				} else {
					pathToUse = locatorPath;
				}
				Bundle bundle;
				try (InputStream in = newInputStream(pathToUse)) {
					bundle = bc.installBundle(locatorPath.toAbsolutePath().toString(), in);
				}

				if (isTemp && pathToUse != null)
					Files.deleteIfExists(pathToUse);
				return bundle;
			}
		} catch (BundleException | IOException e) {
			throw new A2Exception("Cannot install module " + module, e);
		}
	}

	@Override
	public void update(Bundle bundle, A2Module module) {
		try {
			Object locator = module.getLocator();
			if (usingReference && locator instanceof Path) {
				try (InputStream in = newInputStream(locator)) {
					bundle.update(in);
				}
			} else {
				Path locatorPath = (Path) locator;
				Path pathToUse;
				boolean isTemp = false;
				if (locator instanceof Path && Files.isDirectory(locatorPath)) {
					pathToUse = toTempJar(locatorPath);
					isTemp = true;
				} else {
					pathToUse = locatorPath;
				}
				try (InputStream in = newInputStream(pathToUse)) {
					bundle.update(in);
				}
				if (isTemp && pathToUse != null)
					Files.deleteIfExists(pathToUse);
			}
		} catch (BundleException | IOException e) {
			throw new A2Exception("Cannot update module " + module, e);
		}
	}

	@Override
	public A2Branch findBranch(String componentId, Version version) {
		A2Component component = findComponent(componentId);
		if (component == null)
			return null;
		String branchId = version.getMajor() + "." + version.getMinor();
		if (!component.branches.containsKey(branchId))
			return null;
		return component.branches.get(branchId);
	}

	protected A2Contribution getOrAddContribution(String contributionId) {
		if (contributions.containsKey(contributionId))
			return contributions.get(contributionId);
		else {
			A2Contribution contribution = new A2Contribution(this, contributionId);
			contributions.put(contributionId, contribution);
			return contribution;
		}
	}

	protected void asTree(String prefix, StringBuffer buf) {
		if (prefix == null)
			prefix = "";
		for (String contributionId : contributions.keySet()) {
			buf.append(prefix);
			buf.append(contributionId);
			buf.append('\n');
			A2Contribution contribution = contributions.get(contributionId);
			contribution.asTree(prefix + " ", buf);
		}
	}

	protected void asTree() {
		StringBuffer buf = new StringBuffer();
		asTree("", buf);
		System.out.println(buf);
	}

	protected A2Component findComponent(String componentId) {
		SortedMap<A2Contribution, A2Component> res = new TreeMap<>();
		for (A2Contribution contribution : contributions.values()) {
			components: for (String componentIdKey : contribution.components.keySet()) {
				if (componentId.equals(componentIdKey)) {
					res.put(contribution, contribution.components.get(componentIdKey));
					break components;
				}
			}
		}
		if (res.size() == 0)
			return null;
		// TODO explicit contribution priorities
		return res.get(res.lastKey());

	}

	protected String[] readNameVersionFromModule(Path modulePath) {
		Manifest manifest;
		if (Files.isDirectory(modulePath)) {
			manifest = findManifest(modulePath);
		} else {
			try (JarInputStream in = new JarInputStream(newInputStream(modulePath))) {
				manifest = in.getManifest();
			} catch (IOException e) {
				throw new A2Exception("Cannot read manifest from " + modulePath, e);
			}
		}
		String versionStr = manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
		String symbolicName = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
		int semiColIndex = symbolicName.indexOf(';');
		if (semiColIndex >= 0)
			symbolicName = symbolicName.substring(0, semiColIndex);
		return new String[] { symbolicName, versionStr };
	}

	protected String readVersionFromModule(Path modulePath) {
		Manifest manifest;
		if (Files.isDirectory(modulePath)) {
			manifest = findManifest(modulePath);
		} else {
			try (JarInputStream in = new JarInputStream(newInputStream(modulePath))) {
				manifest = in.getManifest();
			} catch (IOException e) {
				throw new A2Exception("Cannot read manifest from " + modulePath, e);
			}
		}
		String versionStr = manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
		return versionStr;
	}

	protected String readSymbolicNameFromModule(Path modulePath) {
		Manifest manifest;
		if (Files.isDirectory(modulePath)) {
			manifest = findManifest(modulePath);
		} else {
			try (JarInputStream in = new JarInputStream(newInputStream(modulePath))) {
				manifest = in.getManifest();
			} catch (IOException e) {
				throw new A2Exception("Cannot read manifest from " + modulePath, e);
			}
		}
		String symbolicName = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
		int semiColIndex = symbolicName.indexOf(';');
		if (semiColIndex >= 0)
			symbolicName = symbolicName.substring(0, semiColIndex);
		return symbolicName;
	}

	protected boolean isUsingReference() {
		return usingReference;
	}

	private InputStream newInputStream(Object locator) throws IOException {
		if (locator instanceof Path) {
			return Files.newInputStream((Path) locator);
		} else if (locator instanceof URL) {
			return ((URL) locator).openStream();
		} else {
			throw new IllegalArgumentException("Unsupported module locator type " + locator.getClass());
		}
	}

	private static Manifest findManifest(Path currentPath) {
		Path metaInfPath = currentPath.resolve("META-INF");
		if (Files.exists(metaInfPath) && Files.isDirectory(metaInfPath)) {
			Path manifestPath = metaInfPath.resolve("MANIFEST.MF");
			try {
				try (InputStream in = Files.newInputStream(manifestPath)) {
					Manifest manifest = new Manifest(in);
					return manifest;
				}
			} catch (IOException e) {
				throw new A2Exception("Cannot read manifest from " + manifestPath, e);
			}
		} else {
			Path parentPath = currentPath.getParent();
			if (parentPath == null)
				throw new A2Exception("MANIFEST.MF file not found.");
			return findManifest(currentPath.getParent());
		}
	}

	private static Path toTempJar(Path dir) {
		try {
			Manifest manifest = findManifest(dir);
			Path jarPath = Files.createTempFile("a2Source", ".jar");
			try (JarOutputStream zos = new JarOutputStream(new FileOutputStream(jarPath.toFile()), manifest)) {
				Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Path relPath = dir.relativize(file);
						// skip MANIFEST from folder
						if (relPath.toString().contentEquals("META-INF/MANIFEST.MF"))
							return FileVisitResult.CONTINUE;
						zos.putNextEntry(new ZipEntry(relPath.toString()));
						Files.copy(file, zos);
						zos.closeEntry();
						return FileVisitResult.CONTINUE;
					}
				});
			}
			return jarPath;
		} catch (IOException e) {
			throw new A2Exception("Cannot install OSGi bundle from " + dir, e);
		}

	}

}
