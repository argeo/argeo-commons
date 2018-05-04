package org.argeo.osgi.boot.a2;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

class A2Contribution implements Comparable<A2Contribution> {
	final static String BOOT = "boot";
	final static String RUNTIME = "runtime";

	private final ProvisioningSource source;
	private final String id;

	final Map<String, A2Component> components = Collections.synchronizedSortedMap(new TreeMap<>());

	public A2Contribution(ProvisioningSource context, String id) {
		this.source = context;
		this.id = id;
		if (context != null)
			context.contributions.put(id, this);
	}

	A2Component getOrAddComponent(String componentId) {
		if (components.containsKey(componentId))
			return components.get(componentId);
		else
			return new A2Component(this, componentId);
	}

	public ProvisioningSource getSource() {
		return source;
	}

	public String getId() {
		return id;
	}

	@Override
	public int compareTo(A2Contribution o) {
		return id.compareTo(o.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof A2Contribution) {
			A2Contribution o = (A2Contribution) obj;
			return id.equals(o.id);
		} else
			return false;
	}

	@Override
	public String toString() {
		return id;
	}

	void asTree(String prefix, StringBuffer buf) {
		if (prefix == null)
			prefix = "";
		for (String componentId : components.keySet()) {
			buf.append(prefix);
			buf.append(componentId);
			A2Component component = components.get(componentId);
			component.asTree(prefix, buf);
			buf.append('\n');
		}
	}

}
