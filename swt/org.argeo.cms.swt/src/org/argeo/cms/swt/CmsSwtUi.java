package org.argeo.cms.swt;

import java.util.TimerTask;

import org.argeo.api.cms.ux.CmsUi;
import org.argeo.api.cms.ux.CmsView;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/** A basic {@link CmsUi}, based on an SWT {@link Composite}. */
public class CmsSwtUi extends Composite implements CmsUi {

	private static final long serialVersionUID = -107939076610406448L;

	/** Last time the UI was accessed. */
	private long lastAccess = System.currentTimeMillis();
	private TimerTask timeoutTask;

	private CmsView cmsView;

	public CmsSwtUi(Composite parent, int style) {
		super(parent, style);
		cmsView = CmsSwtUtils.getCmsView(parent);
		// TODO make timeout configurable
		setUiTimeout(12 * 60 * 60 * 1000);// 12 hours
		setLayout(new GridLayout());
	}

	@Override
	public CmsView getCmsView() {
		return cmsView;
	}

	@Override
	public long getLastAccess() {
		return lastAccess;
	}

	@Override
	public void updateLastAccess() {
		this.lastAccess = System.currentTimeMillis();
	}

	public void setUiTimeout(long uiTimeout) {
		if (timeoutTask != null)
			timeoutTask.cancel();
		timeoutTask = cmsView.schedule(() -> {
			if (System.currentTimeMillis() - getLastAccess() >= uiTimeout)
				dispose();
		}, 0, 1000);
	}

}