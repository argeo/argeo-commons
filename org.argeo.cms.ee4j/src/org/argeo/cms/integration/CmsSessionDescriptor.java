package org.argeo.cms.integration;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.argeo.api.cms.CmsSession;
import org.osgi.service.useradmin.Authorization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A serializable descriptor of an internal {@link CmsSession}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CmsSessionDescriptor implements Serializable, Authorization {
	private static final long serialVersionUID = 8592162323372641462L;

	private String name;
	private String cmsSessionId;
	private String displayName;
	private String locale;
	private Set<String> roles;

	public CmsSessionDescriptor() {
	}

	public CmsSessionDescriptor(String name, String cmsSessionId, String[] roles, String displayName, String locale) {
		this.name = name;
		this.displayName = displayName;
		this.cmsSessionId = cmsSessionId;
		this.locale = locale;
		this.roles = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(roles)));
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getCmsSessionId() {
		return cmsSessionId;
	}

	public void setCmsSessionId(String cmsSessionId) {
		this.cmsSessionId = cmsSessionId;
	}

	public Boolean isAnonymous() {
		return name == null;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	@Override
	public boolean hasRole(String name) {
		return roles.contains(name);
	}

	@Override
	public String[] getRoles() {
		return roles.toArray(new String[roles.size()]);
	}

	public void setRoles(String[] roles) {
		this.roles = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(roles)));
	}

	@Override
	public int hashCode() {
		return cmsSessionId != null ? cmsSessionId.hashCode() : super.hashCode();
	}

	@Override
	public String toString() {
		return displayName != null ? displayName : name != null ? name : super.toString();
	}

}
