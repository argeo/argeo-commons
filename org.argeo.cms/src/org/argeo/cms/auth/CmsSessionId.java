package org.argeo.cms.auth;

import java.util.UUID;

import javax.security.auth.Subject;

import org.argeo.cms.CmsException;

/**
 * The ID of a {@link CmsSession}, which must be available in the private
 * credentials of an authenticated {@link Subject}.
 */
public class CmsSessionId {
	private final UUID uuid;

	public CmsSessionId(UUID value) {
		if (value == null)
			throw new CmsException("value cannot be null");
		this.uuid = value;
	}

	public UUID getUuid() {
		return uuid;
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof CmsSessionId && ((CmsSessionId) obj).getUuid().equals(uuid);
	}

	@Override
	public String toString() {
		return "Node Session " + uuid;
	}

}
