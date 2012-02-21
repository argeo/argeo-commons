package org.argeo.security.ui.views;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.argeo.ArgeoLogListener;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

/** A content provider maintaing an array of lines */
class LogContentProvider implements ILazyContentProvider, ArgeoLogListener {
	private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	private final TableViewer viewer;
	private List<String> lines;

	public LogContentProvider(TableViewer viewer) {
		this.viewer = viewer;
	}

	public synchronized void dispose() {
		lines = null;
	}

	@SuppressWarnings("unchecked")
	public synchronized void inputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
		lines = (List<String>) newInput;
		if (lines == null)
			return;
		this.viewer.setItemCount(lines.size());
	}

	public void updateElement(int index) {
		viewer.replace(lines.get(index), index);
	}

	public synchronized void appendLog(String username, Long timestamp,
			String level, String category, String thread, Object msg,
			String[] exception) {
		// check if valid
		if (lines == null)
			return;

		String message = msg.toString();
		StringBuffer buf = new StringBuffer("");
		buf.append(dateFormat.format(new Date(timestamp))).append(" ");
		buf.append(level).append(" ");
		int count = 0;
		String lastLine = null;
		for (String line : message.split("\n")) {
			if (count == 0)
				lastLine = buf + line;
			else
				lastLine = line;
			lines.add(lastLine);
			count++;
		}

		if (exception != null) {
			for (String ste : exception) {
				lastLine = ste;
				lines.add(lastLine);
			}
		}
		final Object lastElement = lastLine;
		viewer.getTable().getDisplay().asyncExec(new Runnable() {
			public void run() {
				viewer.setItemCount(lines.size());
				if (lastElement != null)
					viewer.reveal(lastElement);
			}
		});
	}
	// private class LogLine {
	// private String message;
	// }
}