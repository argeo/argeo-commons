package org.argeo.security.ui.views;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.argeo.ArgeoLogListener;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/** A content provider maintaining an array of lines */
class LogContentProvider implements ILazyContentProvider, ArgeoLogListener {
	private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	private final Long start;
	/** current - start = line number. first line is number '1' */
	private Long current;

	// TODO make it configurable
	private final Integer maxLineBufferSize = 10 * 1000;

	private final TableViewer viewer;
	private final LinkedList<LogLine> lines;

	public LogContentProvider(TableViewer viewer) {
		this.viewer = viewer;
		start = System.currentTimeMillis();
		lines = new LinkedList<LogLine>();
		current = start;
	}

	public synchronized void dispose() {
		lines.clear();
	}

	@SuppressWarnings("unchecked")
	public synchronized void inputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
		List<String> lin = (List<String>) newInput;
		if (lin == null)
			return;
		for (String line : lin) {
			addLine(line);
		}
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
		for (String line : message.split("\n")) {
			addLine(count == 0 ? buf + line : line);
			count++;
		}

		if (exception != null) {
			for (String ste : exception) {
				addLine(ste);
			}
		}
		viewer.getTable().getDisplay().syncExec(new Runnable() {
			public void run() {
				viewer.setItemCount(lines.size());
				// viewer.reveal(lines.peekLast());
				Table table = viewer.getTable();
				// table.setTopIndex(lines.size()-1);
				System.out.println("topIndex=" + table.getTopIndex()
						+ ", tableSize=" + lines.size());
				// table.select(lines.size() - 1);
				// table.showSelection();
				TableItem ti = table.getItem(lines.size() - 1);
				if (ti == null)
					System.out.println("tableItem is null");
				table.showItem(ti);
			}
		});
	}

	protected synchronized LogLine addLine(String line) {
		// check for maximal size and purge if necessary
		while (lines.size() >= maxLineBufferSize) {
			for (int i = 0; i < maxLineBufferSize / 10; i++) {
				lines.poll();
			}
		}

		current++;
		LogLine logLine = new LogLine(current, line);
		lines.add(logLine);
		return logLine;
	}

	private class LogLine {
		private Long linenumber;
		private String message;

		public LogLine(Long linenumber, String message) {
			this.linenumber = linenumber;
			this.message = message;
		}

		@Override
		public int hashCode() {
			return linenumber.intValue();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof LogLine)
				return ((LogLine) obj).linenumber.equals(linenumber);
			else
				return false;
		}

		@Override
		public String toString() {
			return message;
		}

	}
}