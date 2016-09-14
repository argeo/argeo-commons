/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.security.ui.views;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.argeo.node.ArgeoLogListener;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/** A content provider maintaining an array of lines */
class LogContentProvider implements ILazyContentProvider, ArgeoLogListener {
	private static final long serialVersionUID = -2084872367738339721L;

	private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	private final Long start;
	/** current - start = line number. first line is number '1' */
	private Long current;

	// TODO make it configurable
	private final Integer maxLineBufferSize = 10 * 1000;

	private final TableViewer viewer;
	private LinkedList<LogLine> lines;

	public LogContentProvider(TableViewer viewer) {
		this.viewer = viewer;
		start = System.currentTimeMillis();
		lines = new LinkedList<LogLine>();
		current = start;
	}

	public synchronized void dispose() {
		lines.clear();
		lines = null;
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
		int count = 0;
		String prefix = prefix(username, timestamp, level, category, thread)
				.toString();
		// String suffix = suffix(username, timestamp, level, category, thread);
		for (String line : message.split("\n")) {
			addLine(count == 0 ? prefix + line : line);
			count++;
		}

		if (exception != null) {
			for (String ste : exception) {
				addLine(ste);
			}
		}

		viewer.getTable().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (lines == null)
					return;
				viewer.setItemCount(lines.size());
				// doesn't work with syncExec
				scrollToLastLine();
			}
		});
	}

	protected StringBuffer prefix(String username, Long timestamp,
			String level, String category, String thread) {
		StringBuffer buf = new StringBuffer("");
		buf.append(dateFormat.format(new Date(timestamp))).append(" ");
		// buf.append(level).append(" ");
		return buf;
	}

	/** Normalize string to the given size */
	protected String norm(String str, Integer size) {
		int length = str.length();
		if (length == size)
			return str;
		else if (length > size)
			return str.substring(0, size);
		else {
			char[] arr = new char[size - length];
			Arrays.fill(arr, ' ');
			return str + new String(arr);
		}
	}

	// protected String suffix(String username, Long timestamp, String level,
	// String category, String thread) {
	// return "";
	// }

	/** Scroll to the last line */
	protected synchronized void scrollToLastLine() {
		// we try to show last line with two methods
		// viewer.reveal(lines.peekLast());

		Table table = viewer.getTable();
		TableItem ti = table.getItem(table.getItemCount() - 1);
		table.showItem(ti);
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