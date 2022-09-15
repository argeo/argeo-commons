package org.argeo.util.dav;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

public class DavResponse {
	final static String MOD_DAV_NAMESPACE = "http://apache.org/dav/props/";

	private String href;
	private boolean collection;
	private Set<QName> propertyNames = new HashSet<>();
	private Map<QName, String> properties = new HashMap<>();
	private List<QName> resourceTypes = new ArrayList<>();

	public Map<QName, String> getProperties() {
		return properties;
	}

	void setHref(String href) {
		this.href = href;
	}

	public String getHref() {
		return href;
	}

	public boolean isCollection() {
		return collection;
	}

	void setCollection(boolean collection) {
		this.collection = collection;
	}

	public List<QName> getResourceTypes() {
		return resourceTypes;
	}

	public Set<QName> getPropertyNames() {
		return propertyNames;
	}

}
