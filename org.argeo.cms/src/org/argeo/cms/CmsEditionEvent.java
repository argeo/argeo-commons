package org.argeo.cms;

import java.util.EventObject;

/** Notify of the edition lifecycle */
public class CmsEditionEvent extends EventObject {
	private static final long serialVersionUID = 950914736016693110L;

	public final static Integer START_EDITING = 0;
	public final static Integer STOP_EDITING = 1;

	private final Integer type;

	public CmsEditionEvent(Object source, Integer type) {
		super(source);
		this.type = type;
	}

	public Integer getType() {
		return type;
	}

}
