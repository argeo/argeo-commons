package org.argeo.eclipse.ui.specific;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;

public class FileDropAdapter {

	public void prepareDropTarget(Control control, DropTarget dropTarget) {
		dropTarget.setTransfer(new Transfer[] { FileTransfer.getInstance() });
		dropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void dropAccept(DropTargetEvent event) {
				if (!FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
					event.detail = DND.DROP_NONE;
				}
			}

			@Override
			public void drop(DropTargetEvent event) {
				handleFileDrop(control, event);
			}
		});
	}

	public void handleFileDrop(Control control, DropTargetEvent event) {
		String fileList[] = null;
		FileTransfer ft = FileTransfer.getInstance();
		if (ft.isSupportedType(event.currentDataType)) {
			fileList = (String[]) event.data;
		}
		System.out.println(Arrays.toString(fileList));
	}

	/** Executed in UI thread */
	protected void processUpload(InputStream in, String fileName, String contentType) throws IOException {

	}

}
