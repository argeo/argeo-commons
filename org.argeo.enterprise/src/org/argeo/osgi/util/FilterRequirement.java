package org.argeo.osgi.util;

import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class FilterRequirement implements Requirement {
	private String namespace;
	private String filter;
	
	

	public FilterRequirement(String namespace, String filter) {
		this.namespace = namespace;
		this.filter = filter;
	}

	@Override
	public Resource getResource() {
		return null;
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public Map<String, String> getDirectives() {
		Map<String, String> directives = new HashMap<>();
		directives.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
		return directives;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return new HashMap<>();
	}

}
