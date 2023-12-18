package org.argeo.cms.swt;

import org.argeo.api.cms.ux.CmsUi;
import org.argeo.api.cms.ux.CmsView;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/** A basic {@link CmsUi}, based on an SWT {@link Composite}. */
public class CmsSwtUi extends Composite implements CmsUi {

	private static final long serialVersionUID = -107939076610406448L;

	/** Last time the UI was accessed. */
	private long lastAccess = System.currentTimeMillis();
//	private TimerTask timeoutTask;
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
	public void updateLastAccess() {
		this.lastAccess = System.currentTimeMillis();
	}

	public void setUiTimeout(long uiTimeout) {
//		clearTimeoutTask();
		this.uiTimeout = uiTimeout;
		if (this.uiTimeout <= 0)
			return;
		// TODO introduce mechanism to check whether the UI is "zombie"
		// (that is the UI thread still exists, but cannot execute anything)
//		final long timeoutTaskPeriod = 60 * 60 * 1000;// 1h
//		timeoutTask = cmsView.schedule(() -> {
//			disposeIfTimedout();
//		}, timeoutTaskPeriod, timeoutTaskPeriod);
//		addDisposeListener((e) -> {
//			clearTimeoutTask();
//		});
	}

//	/** Must be run in UI thread. */
//	public void disposeIfTimedout() {
//		System.out.println("Enter disposeIfTimedout");
//		if (isDisposed()) {
//			clearTimeoutTask();
//			return;
//		}
//		if (isTimedOut()) {
//			dispose();
//			clearTimeoutTask();
//			System.out.println("Disposed after timeout");
//		}
//	}

//	private void clearTimeoutTask() {
//		if (timeoutTask != null) {
//			timeoutTask.cancel();
//			timeoutTask = null;
//		}
//	}

	@Override
	public boolean isTimedOut() {
		return uiTimeout > 0 && (System.currentTimeMillis() - lastAccess >= uiTimeout);
	}

//	class DisposeIfTimedOutTask implements Runnable {
//		public void run() {
//			disposeIfTimedout();
//			getDisplay().timerExec(1000, new DisposeIfTimedOutTask());
//		}
//
//	}

}