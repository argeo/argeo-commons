package org.argeo.api.cms;

/**
 * Can be applied to {@link Enum}s in order to define events used by
 * {@link CmsView#sendEvent(String, java.util.Map)}.
 */
public interface CmsEvent {
	String name();

	default String topic() {
		return getTopicBase() + "/" + name();
	}

	default 	String getTopicBase() {
		return "argeo/cms";
	}


}
