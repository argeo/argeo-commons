package org.argeo.osgi.useradmin;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.osgi.service.useradmin.UserAdmin;

public abstract class AbstractLdapUserAdmin implements UserAdmin {
	private boolean isReadOnly;
	private URI uri;

	public AbstractLdapUserAdmin() {
	}

	public AbstractLdapUserAdmin(URI uri, boolean isReadOnly) {
		this.uri = uri;
		this.isReadOnly = isReadOnly;
	}

	private List<String> indexedUserProperties = Arrays.asList(new String[] {
			"uid", "mail", "cn" });

	protected URI getUri() {
		return uri;
	}

	protected void setUri(URI uri) {
		this.uri = uri;
	}

	protected List<String> getIndexedUserProperties() {
		return indexedUserProperties;
	}

	protected void setIndexedUserProperties(List<String> indexedUserProperties) {
		this.indexedUserProperties = indexedUserProperties;
	}

	protected void setReadOnly(boolean isReadOnly) {
		this.isReadOnly = isReadOnly;
	}

	public boolean isReadOnly() {
		return isReadOnly;
	}

	public void destroy() {

	}

}
