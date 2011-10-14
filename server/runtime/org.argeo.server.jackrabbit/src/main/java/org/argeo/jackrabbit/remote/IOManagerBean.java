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
