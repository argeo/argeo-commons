package org.argeo.security;

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonMethod;

//@JsonAutoDetect(value = { JsonMethod.GETTER, JsonMethod.SETTER })
public class UserNature {
	private final static Log log = LogFactory.getLog(UserNature.class);

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

	@JsonAnySetter
	public void anySetter(String key, Object obj) {
		if (obj != null)
			log.info("anySetter: " + key + "=" + obj + " (" + obj.getClass()
					+ "), natureType=" + type);
	}

	@JsonCreator
	public static Object valueOf(String str) {
		log.info("create: " + str);
		return new UserNature();
	}
}
