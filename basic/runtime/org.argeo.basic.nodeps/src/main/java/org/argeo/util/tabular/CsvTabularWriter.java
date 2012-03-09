/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.util.tabular;

import java.io.OutputStream;

import org.argeo.util.CsvWriter;

/** Write tabular content in a stream as CSV. Wraps a {@link CsvWriter}. */
public class CsvTabularWriter implements TabularWriter {
	private CsvWriter csvWriter;

	public CsvTabularWriter(OutputStream out) {
		this.csvWriter = new CsvWriter(out);
	}

	public void appendRow(Object[] row) {
		csvWriter.writeLine(row);
	}

	public void close() {
	}

}
