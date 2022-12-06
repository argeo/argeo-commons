package org.argeo.api.cms.ux;

public interface CmsEditionListener {
	void editionStarted(CmsEditionEvent e);

	void editionStopped(CmsEditionEvent e);
}
