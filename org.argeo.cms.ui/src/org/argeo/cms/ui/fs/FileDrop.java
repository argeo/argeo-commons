package org.argeo.cms.ui.fs;

import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.specific.FileDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Control;

/** Allows a control to receive file drops. */
public class FileDrop {
	private final static Log log = LogFactory.getLog(FileDrop.class);

	public void createDropTarget(Control control) {
		FileDropAdapter fileDropAdapter = new FileDropAdapter() {
			@Override
			protected void processUpload(InputStream in, String fileName, String contentType) {
				if (log.isDebugEnabled())
					log.debug("Process upload of " + fileName + " (" + contentType + ")");
				processUpload(in, fileName, contentType);
			}
		};
		DropTarget dropTarget = new DropTarget(control, DND.DROP_MOVE | DND.DROP_COPY);
		fileDropAdapter.prepareDropTarget(control, dropTarget);
	}

	public void handleFileDrop(Control control, DropTargetEvent event) {
	}

	/** Executed in UI thread */
	protected void processUpload(InputStream in, String fileName, String contentType) {

	}
}
