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
	private long uiTimeout = 0;

	private CmsView cmsView;

	public CmsSwtUi(Composite parent, int style) {
		super(parent, style);
		cmsView = CmsSwtUtils.getCmsView(parent);
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
		this.uiTimeout = uiTimeout;
		if (this.uiTimeout <= 0)
			return;
		timeoutTask = cmsView.schedule(() -> {
			disposeIfTimedout();
		}, 0, 60 * 1000);
	}

	public void disposeIfTimedout() {
		if (timeoutTask == null)
			return;
		if (isDisposed()) {
			timeoutTask.cancel();
			timeoutTask = null;
			return;
		}
		if (System.currentTimeMillis() - getLastAccess() >= uiTimeout) {
			timeoutTask.cancel();
			timeoutTask = null;
			dispose();
		}
	}

}