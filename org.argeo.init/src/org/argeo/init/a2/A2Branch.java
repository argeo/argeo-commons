package org.argeo.init.a2;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import org.argeo.init.osgi.OsgiBootUtils;
import org.osgi.framework.Version;

/**
 * A logical linear sequence of versions of a given {@link A2Component}. This is
 * typically a combination of major and minor version, indicating backward
 * compatibility.
 */
public class A2Branch implements Comparable<A2Branch> {
	private final A2Component component;
	private final String id;

	final SortedMap<Version, A2Module> modules = Collections.synchronizedSortedMap(new TreeMap<>());

	public A2Branch(A2Component component, String id) {
		this.component = component;
		this.id = id;
		component.branches.put(id, this);
	}

	A2Module getOrAddModule(Version version, Object locator) {
		if (modules.containsKey(version)) {
			A2Module res = modules.get(version);
			if (OsgiBootUtils.isDebug() && !res.getLocator().equals(locator)) {
				OsgiBootUtils.debug("Inconsistent locator " + locator + " (registered: " + res.getLocator() + ")");
			}
			return res;
		} else
			return new A2Module(this, version, locator);
	}

	A2Module last() {
		return modules.get(modules.lastKey());
	}

	A2Module first() {
		return modules.get(modules.firstKey());
	}

	A2Component getComponent() {
		return component;
	}

	String getId() {
		return id;
	}

	@Override
	public int compareTo(A2Branch o) {
		return id.compareTo(id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof A2Branch) {
			A2Branch o = (A2Branch) obj;
			return component.equals(o.component) && id.equals(o.id);
		} else
			return false;
	}

	@Override
	public String toString() {
		return getCoordinates();
	}

	public String getCoordinates() {
		return component + ":" + id;
	}

	static String versionToBranchId(Version version) {
		return version.getMajor() + "." + version.getMinor();
	}
}
