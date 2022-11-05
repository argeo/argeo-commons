package org.argeo.cms.dav;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.namespace.QName;

import org.argeo.cms.http.HttpStatus;

/** The WebDav response for a given resource. */
public class DavResponse {
	final static String MOD_DAV_NAMESPACE = "http://apache.org/dav/props/";

	private String href;
	private boolean collection;
	private Map<HttpStatus, Set<QName>> propertyNames = new TreeMap<>();
	private Map<QName, String> properties = new HashMap<>();
	private List<QName> resourceTypes = new ArrayList<>();

	public Map<QName, String> getProperties() {
		return properties;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getHref() {
		return href;
	}

	public boolean isCollection() {
		return collection;
	}

	public void setCollection(boolean collection) {
		this.collection = collection;
	}

	public List<QName> getResourceTypes() {
		return resourceTypes;
	}

	public Set<QName> getPropertyNames(HttpStatus status) {
		if (!propertyNames.containsKey(status))
			propertyNames.put(status, new TreeSet<>(DavXmlElement.QNAME_COMPARATOR));
		return propertyNames.get(status);
	}

	public Set<HttpStatus> getStatuses() {
		return propertyNames.keySet();
	}

}
