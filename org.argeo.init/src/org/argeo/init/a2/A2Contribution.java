package org.argeo.init.a2;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * A category grouping a set of {@link A2Component}, typically based on the
 * provider of these components. This is the equivalent of Maven's group Id.
 */
public class A2Contribution implements Comparable<A2Contribution> {
	final static String BOOT = "boot";
	final static String RUNTIME = "runtime";
	final static String CLASSPATH = "classpath";

	private final ProvisioningSource source;
	private final String id;

	final Map<String, A2Component> components = Collections.synchronizedSortedMap(new TreeMap<>());

	/**
	 * The contribution must be added to the source. Rather use
	 * {@link AbstractProvisioningSource#getOrAddContribution(String)} than this
	 * contructor directly.
	 */
	public A2Contribution(ProvisioningSource context, String id) {
		this.source = context;
		this.id = id;
//		if (context != null)
//			context.contributions.put(id, this);
	}

	public Iterable<A2Component> listComponents(Object filter) {
		return components.values();
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
