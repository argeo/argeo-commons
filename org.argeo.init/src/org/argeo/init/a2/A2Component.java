package org.argeo.init.a2;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import org.osgi.framework.Version;

/**
 * The logical name of a software package. In OSGi's case this is
 * <code>Bundle-SymbolicName</code>. This is the equivalent of Maven's artifact
 * id.
 */
public class A2Component implements Comparable<A2Component> {
	private final A2Contribution contribution;
	private final String id;

	final SortedMap<String, A2Branch> branches = Collections.synchronizedSortedMap(new TreeMap<>());

	public A2Component(A2Contribution contribution, String id) {
		this.contribution = contribution;
		this.id = id;
		contribution.components.put(id, this);
	}

	public Iterable<A2Branch> listBranches(Object filter) {
		return branches.values();
	}

	A2Branch getOrAddBranch(String branchId) {
		if (!branches.containsKey(branchId)) {
			A2Branch a2Branch = new A2Branch(this, branchId);
			branches.put(branchId, a2Branch);
		}
		return branches.get(branchId);
	}

	A2Module getOrAddModule(Version version, Object locator) {
		A2Branch branch = getOrAddBranch(A2Branch.versionToBranchId(version));
		A2Module module = branch.getOrAddModule(version, locator);
		return module;
	}

	public A2Branch last() {
		return branches.get(branches.lastKey());
	}

	public A2Contribution getContribution() {
		return contribution;
	}

	public String getId() {
		return id;
	}

	@Override
	public int compareTo(A2Component o) {
		return id.compareTo(o.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof A2Component) {
			A2Component o = (A2Component) obj;
			return contribution.equals(o.contribution) && id.equals(o.id);
		} else
			return false;
	}

	@Override
	public String toString() {
		return contribution.getId() + ":" + id;
	}

	void asTree(String prefix, StringBuffer buf) {
		if (prefix == null)
			prefix = "";
		A2Branch lastBranch = last();
		SortedMap<String, A2Branch> displayMap = new TreeMap<>(Collections.reverseOrder());
		displayMap.putAll(branches);
		for (String branchId : displayMap.keySet()) {
			A2Branch branch = displayMap.get(branchId);
			if (!lastBranch.equals(branch)) {
				buf.append('\n');
				buf.append(prefix);
			} else {
				buf.append(" -");
			}
			buf.append(prefix);
			buf.append(branchId);
			A2Module first = branch.first();
			A2Module last = branch.last();
			buf.append(" (").append(last.getVersion());
			if (!first.equals(last))
				buf.append(" ... ").append(first.getVersion());
			buf.append(')');
		}
	}

}
