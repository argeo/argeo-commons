package org.argeo.security;

import java.io.Serializable;
import java.util.UUID;

//@JsonAutoDetect(value = { JsonMethod.GETTER, JsonMethod.SETTER })
public class UserNature implements Serializable {
	private static final long serialVersionUID = 1L;

	// private final static Log log = LogFactory.getLog(UserNature.class);

	private String uuid = UUID.randomUUID().toString();
	private String type;

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getType() {
		if (type != null)
			return type;
		else
			return getClass().getName();
	}

	public void setType(String type) {
		this.type = type;
	}

	// @JsonAnySetter
	// public void anySetter(String key, Object obj) {
	// if (obj != null)
	// log.info("anySetter: " + key + "=" + obj + " (" + obj.getClass()
	// + "), natureType=" + type);
	// }
}
