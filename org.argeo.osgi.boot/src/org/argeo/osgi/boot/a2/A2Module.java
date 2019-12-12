package org.argeo.osgi.boot.a2;

import org.osgi.framework.Version;

/**
 * An identified software package. In OSGi's case this is the combination of
 * <code>Bundle-SymbolicName</code> and <code>Bundle-version</code>. This is the
 * equivalent of the full coordinates of a Maven artifact version.
 */
class A2Module implements Comparable<A2Module> {
	private final A2Branch branch;
	private final Version version;
	private final Object locator;

	public A2Module(A2Branch branch, Version version, Object locator) {
		this.branch = branch;
		this.version = version;
		this.locator = locator;
		branch.modules.put(version, this);
	}

	A2Branch getBranch() {
		return branch;
	}

	Version getVersion() {
		return version;
	}

	Object getLocator() {
		return locator;
	}

	@Override
	public int compareTo(A2Module o) {
		return version.compareTo(o.version);
	}

	@Override
	public int hashCode() {
		return version.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof A2Module) {
			A2Module o = (A2Module) obj;
			return branch.equals(o.branch) && version.equals(o.version);
		} else
			return false;
	}

	@Override
	public String toString() {
		return getCoordinates();
	}

	public String getCoordinates() {
		return branch.getComponent() + ":" + version;
	}

}
