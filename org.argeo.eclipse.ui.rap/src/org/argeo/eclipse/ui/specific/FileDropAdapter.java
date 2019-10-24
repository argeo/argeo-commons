package org.argeo.eclipse.ui.specific;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rap.fileupload.FileDetails;
import org.eclipse.rap.fileupload.FileUploadHandler;
import org.eclipse.rap.fileupload.FileUploadReceiver;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.ClientFile;
import org.eclipse.rap.rwt.client.service.ClientFileUploader;
import org.eclipse.rap.rwt.dnd.ClientFileTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;

/** Configures a {@link Control} to receive files drop from the client OS. */
public class FileDropAdapter {

	public void prepareDropTarget(Control control, DropTarget dropTarget) {
		dropTarget.setTransfer(new Transfer[] { ClientFileTransfer.getInstance() });
		dropTarget.addDropListener(new DropTargetAdapter() {
			private static final long serialVersionUID = 5361645765549463168L;

			@Override
			public void dropAccept(DropTargetEvent event) {
				if (!ClientFileTransfer.getInstance().isSupportedType(event.currentDataType)) {
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
		ClientFile[] clientFiles = (ClientFile[]) event.data;
		ClientFileUploader service = RWT.getClient().getService(ClientFileUploader.class);
//		DiskFileUploadReceiver receiver = new DiskFileUploadReceiver();
		FileUploadReceiver receiver = new FileUploadReceiver() {

			@Override
			public void receive(InputStream stream, FileDetails details) throws IOException {
				control.getDisplay().syncExec(() -> {
					try {
						processUpload(stream, details.getFileName(), details.getContentType());
					} catch (IOException e) {
						throw new IllegalStateException("Cannot process upload of " + details.getFileName(), e);
					}
				});
			}
		};
		FileUploadHandler handler = new FileUploadHandler(receiver);
//		    handler.setMaxFileSize( sizeLimit );
//		    handler.setUploadTimeLimit( timeLimit );
		service.submit(handler.getUploadUrl(), clientFiles);
//		for (File file : receiver.getTargetFiles()) {
//			paths.add(file.toPath());
//		}
	}

	/** Executed in UI thread */
	protected void processUpload(InputStream in, String fileName, String contentType) throws IOException {

	}

}
