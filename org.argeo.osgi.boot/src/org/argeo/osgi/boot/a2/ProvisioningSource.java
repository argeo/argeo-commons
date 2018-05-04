package org.argeo.osgi.boot.a2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.argeo.osgi.boot.OsgiBootException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

abstract class ProvisioningSource {
	final Map<String, A2Contribution> contributions = Collections.synchronizedSortedMap(new TreeMap<>());

	A2Contribution getOrAddContribution(String contributionId) {
		if (contributions.containsKey(contributionId))
			return contributions.get(contributionId);
		else
			return new A2Contribution(this, contributionId);
	}

	void asTree(String prefix, StringBuffer buf) {
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

	void asTree() {
		StringBuffer buf = new StringBuffer();
		asTree("", buf);
		System.out.println(buf);
	}

	A2Component findComponent(String componentId) {
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

	A2Branch findBranch(String componentId, Version version) {
		A2Component component = findComponent(componentId);
		if (component == null)
			return null;
		String branchId = version.getMajor() + "." + version.getMinor();
		if (!component.branches.containsKey(branchId))
			return null;
		return component.branches.get(branchId);
	}

	protected String readVersionFromModule(Path modulePath) {
		try (JarInputStream in = new JarInputStream(newInputStream(modulePath))) {
			Manifest manifest = in.getManifest();
			String versionStr = manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
			return versionStr;
		} catch (IOException e) {
			throw new OsgiBootException("Cannot read manifest from " + modulePath, e);
		}
	}

	protected String readSymbolicNameFromModule(Path modulePath) {
		try (JarInputStream in = new JarInputStream(newInputStream(modulePath))) {
			Manifest manifest = in.getManifest();
			String symbolicName = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
			int semiColIndex = symbolicName.indexOf(';');
			if (semiColIndex >= 0)
				symbolicName = symbolicName.substring(0, semiColIndex);
			return symbolicName;
		} catch (IOException e) {
			throw new OsgiBootException("Cannot read manifest from " + modulePath, e);
		}
	}

	InputStream newInputStream(Object locator) throws IOException {
		if (locator instanceof Path) {
			return Files.newInputStream((Path) locator);
		} else if (locator instanceof URL) {
			return ((URL) locator).openStream();
		} else {
			throw new IllegalArgumentException("Unsupported module locator type " + locator.getClass());
		}
	}
}
