package org.argeo.cms.e4.maintenance;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

class LogDeploymentUi extends AbstractOsgiComposite implements LogListener {
	private static final long serialVersionUID = 590221539553514693L;

	private DateFormat dateFormat = new SimpleDateFormat("MMdd HH:mm");

	private Display display;
	private Text logDisplay;

	public LogDeploymentUi(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	protected void initUi(int style) {
		LogReaderService logReader = getService(LogReaderService.class);
		// FIXME use server push
		// logReader.addLogListener(this);
		this.display = getDisplay();
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		logDisplay = new Text(this, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY);
		logDisplay.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		CmsSwtUtils.markup(logDisplay);
		Enumeration<LogEntry> logEntries = (Enumeration<LogEntry>) logReader.getLog();
		while (logEntries.hasMoreElements())
			logDisplay.append(printEntry(logEntries.nextElement()));
	}

	private String printEntry(LogEntry entry) {
		StringBuilder sb = new StringBuilder();
		GregorianCalendar calendar = new GregorianCalendar(TimeZone.getDefault());
		calendar.setTimeInMillis(entry.getTime());
		sb.append(dateFormat.format(calendar.getTime())).append(' ');
		sb.append(entry.getMessage());
		sb.append('\n');
		return sb.toString();
	}

	@Override
	public void logged(LogEntry entry) {
		if (display.isDisposed())
			return;
		display.asyncExec(() -> {
			if (logDisplay.isDisposed())
				return;
			logDisplay.append(printEntry(entry));
		});
		display.wake();
	}

	// @Override
	// public void dispose() {
	// super.dispose();
	// getService(LogReaderService.class).removeLogListener(this);
	// }
}
