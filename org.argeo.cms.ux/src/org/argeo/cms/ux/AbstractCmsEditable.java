package org.argeo.cms.ux;

import java.util.IdentityHashMap;

import org.argeo.api.cms.ux.CmsEditable;
import org.argeo.api.cms.ux.CmsEditionEvent;
import org.argeo.api.cms.ux.CmsEditionListener;

public abstract class AbstractCmsEditable implements CmsEditable {
	private IdentityHashMap<CmsEditionListener, Object> listeners = new IdentityHashMap<>();

	protected void notifyListeners(CmsEditionEvent e) {
		if (CmsEditionEvent.START_EDITING == e.getType()) {
			for (CmsEditionListener listener : listeners.keySet())
				listener.editionStarted(e);
		} else if (CmsEditionEvent.STOP_EDITING == e.getType()) {
			for (CmsEditionListener listener : listeners.keySet())
				listener.editionStopped(e);
		} else {
			throw new IllegalArgumentException("Unkown edition event type " + e.getType());
		}
	}

	@Override
	public void addCmsEditionListener(CmsEditionListener listener) {
		listeners.put(listener, new Object());
	}

	@Override
	public void removeCmsEditionListener(CmsEditionListener listener) {
		listeners.remove(listener, new Object());
	}

}
