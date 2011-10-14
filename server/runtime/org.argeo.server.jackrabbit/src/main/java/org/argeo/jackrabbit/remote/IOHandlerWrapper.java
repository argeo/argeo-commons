package org.argeo.jackrabbit.remote;

import java.io.IOException;

import org.apache.jackrabbit.server.io.ExportContext;
import org.apache.jackrabbit.server.io.IOHandler;
import org.apache.jackrabbit.server.io.IOManager;
import org.apache.jackrabbit.server.io.ImportContext;
import org.apache.jackrabbit.webdav.DavResource;
import org.argeo.ArgeoException;

/** Wraps an IOHandler so that it can be injected a posteriori */
public class IOHandlerWrapper implements IOHandler {
	private IOManager ioManager = null;
	private IOHandler ioHandler = null;

	public void setIOHandler(IOHandler ioHandler) {
		if ((this.ioHandler != null) && (ioHandler != null))
			throw new ArgeoException(
					"There is already an IO Handler registered");
		this.ioHandler = ioHandler;
		if (ioManager != null && this.ioHandler != null)
			ioHandler.setIOManager(ioManager);
	}

	public IOHandler getIOHandler() {
		return ioHandler;
	}

	public IOManager getIOManager() {
		return ioManager;
	}

	public void setIOManager(IOManager ioManager) {
		this.ioManager = ioManager;
		if (ioHandler != null)
			ioHandler.setIOManager(ioManager);
	}

	public String getName() {
		if (ioHandler != null)
			return ioHandler.getName();
		else
			return "Empty IOHandler Wrapper";
	}

	public boolean canImport(ImportContext context, boolean isCollection) {
		if (ioHandler != null)
			return ioHandler.canImport(context, isCollection);
		return false;
	}

	public boolean canImport(ImportContext context, DavResource resource) {
		if (ioHandler != null)
			return ioHandler.canImport(context, resource);
		return false;
	}

	public boolean importContent(ImportContext context, boolean isCollection)
			throws IOException {
		if (ioHandler != null)
			ioHandler.importContent(context, isCollection);
		return false;
	}

	public boolean importContent(ImportContext context, DavResource resource)
			throws IOException {
		if (ioHandler != null)
			ioHandler.importContent(context, resource);
		return false;
	}

	public boolean canExport(ExportContext context, boolean isCollection) {
		if (ioHandler != null)
			ioHandler.canExport(context, isCollection);
		return false;
	}

	public boolean canExport(ExportContext context, DavResource resource) {
		if (ioHandler != null)
			ioHandler.canExport(context, resource);
		return false;
	}

	public boolean exportContent(ExportContext context, boolean isCollection)
			throws IOException {
		if (ioHandler != null)
			ioHandler.exportContent(context, isCollection);
		return false;
	}

	public boolean exportContent(ExportContext context, DavResource resource)
			throws IOException {
		if (ioHandler != null)
			ioHandler.exportContent(context, resource);
		return false;
	}

}
