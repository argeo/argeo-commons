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
package org.argeo.jackrabbit.remote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.server.io.ExportContext;
import org.apache.jackrabbit.server.io.IOHandler;
import org.apache.jackrabbit.server.io.IOManager;
import org.apache.jackrabbit.server.io.ImportContext;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.tika.detect.Detector;

/** {@link IOManager} that can easily be configured as a bean. */
public class IOManagerBean implements IOManager {
	private Detector detector = null;
	private List<IOHandler> ioHandlers = new ArrayList<IOHandler>();

	public boolean importContent(ImportContext context, boolean isCollection)
			throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean importContent(ImportContext context, DavResource resource)
			throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean exportContent(ExportContext context, boolean isCollection)
			throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean exportContent(ExportContext context, DavResource resource)
			throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	public synchronized void addIOHandler(IOHandler ioHandler) {
		ioHandlers.add(ioHandler);
	}

	public synchronized IOHandler[] getIOHandlers() {
		return ioHandlers.toArray(new IOHandler[ioHandlers.size()]);
	}

	public Detector getDetector() {
		return detector;
	}

	public void setDetector(Detector detector) {
		this.detector = detector;
	}

	public synchronized List<IOHandler> getIoHandlers() {
		return ioHandlers;
	}

	public synchronized void setIoHandlers(List<IOHandler> ioHandlers) {
		this.ioHandlers = ioHandlers;
	}

}
