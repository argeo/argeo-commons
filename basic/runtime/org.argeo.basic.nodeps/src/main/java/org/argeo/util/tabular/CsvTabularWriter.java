package org.argeo.util.tabular;

import java.io.OutputStream;
import java.util.List;

import org.argeo.util.CsvWriter;

/** Write tabular content in a stream as CSV. Wraps a {@link CsvWriter}. */
public class CsvTabularWriter implements TabularWriter {
	private CsvWriter csvWriter;

	public CsvTabularWriter(OutputStream out) {
		this.csvWriter = new CsvWriter(out);
	}

	public void appendRow(List<?> row) {
		csvWriter.writeLine(row);
	}

	public void close() {
	}

}
