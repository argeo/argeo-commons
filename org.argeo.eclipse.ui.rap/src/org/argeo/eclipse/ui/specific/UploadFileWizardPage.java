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
package org.argeo.eclipse.ui.specific;

/** @deprecated Legacy, do not use */
public class UploadFileWizardPage {
}


/// **
// * RWT Specific convenience page that provides a simple interface to upload
/// one
// * file in a wizard context
// *
// * TODO Finalize clean and refactoring using the latest rap version and upload
// * dialog addons
// *
// */
// public class UploadFileWizardPage extends WizardPage {
// // private final static Log log = LogFactory
// // .getLog(UploadFileWizardPage.class);
// private static final long serialVersionUID = 8251354244542973179L;
// public final static String FILE_ITEM_TYPE = "FILE";
// public final static String FOLDER_ITEM_TYPE = "FOLDER";
//
// private File file;
//
// private FileUpload fileUpload;
// private ServerPushSession pushSession;
// private Label fileNameLabel;
//
// public UploadFileWizardPage() {
// super("Import from file system");
// setDescription("Import files from the local file system to the server");
// }
//
// public void createControl(Composite parent) {
// Composite composite = new Composite(parent, SWT.NONE);
// composite.setLayout(new GridLayout(3, false));
// composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
// new Label(composite, SWT.NONE).setText("Pick up a file");
//
// fileNameLabel = new Label(composite, SWT.NONE | SWT.BEGINNING);
// fileNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
// false));
//
// fileUpload = new FileUpload(composite, SWT.NONE);
// fileUpload.setText("Browse...");
// fileUpload.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
// false));
//
// final String url = startUploadReceiver();
// pushSession = new ServerPushSession();
//
// fileUpload.addSelectionListener(new SelectionAdapter() {
// private static final long serialVersionUID = 1L;
//
// @Override
// public void widgetSelected(SelectionEvent e) {
// String fileName = fileUpload.getFileName();
// fileNameLabel.setText(fileName == null ? "" : fileName);
// pushSession.start();
// fileUpload.submit(url);
// }
// });
//
// setControl(composite);
// }
//
// public void performFinish() {
// // boolean success = uploadFile.performUpload();
// // if (!success)
// // throw new ArgeoException("Cannot upload file named "
// // + uploadFile.getPath());
// }
//
// private String startUploadReceiver() {
// MyFileUploadReceiver receiver = new MyFileUploadReceiver();
// FileUploadHandler uploadHandler = new FileUploadHandler(receiver);
// uploadHandler.addUploadListener(new FileUploadListener() {
//
// public void uploadProgress(FileUploadEvent event) {
// // handle upload progress
// }
//
// public void uploadFailed(FileUploadEvent event) {
// UploadFileWizardPage.this.setErrorMessage("upload failed: "
// + event.getException());
// }
//
// public void uploadFinished(FileUploadEvent event) {
//
// fileNameLabel.getDisplay().asyncExec(new Runnable() {
// public void run() {
// // UploadFileWizardPage.this.getContainer()
// // .updateButtons();
// pushSession.stop();
// }
// });
//
// // for (FileDetails file : event.getFileDetails()) {
// // // addToLog("received: " + file.getFileName());
// // }
// }
// });
// return uploadHandler.getUploadUrl();
// }
//
// private class MyFileUploadReceiver extends FileUploadReceiver {
//
// private static final String TEMP_FILE_PREFIX = "fileupload_";
//
// @Override
// public void receive(InputStream dataStream, FileDetails details)
// throws IOException {
// File result = File.createTempFile(TEMP_FILE_PREFIX, "");
// FileOutputStream outputStream = new FileOutputStream(result);
// try {
// copy(dataStream, outputStream);
// } finally {
// dataStream.close();
// outputStream.close();
// }
// file = result;
// }
// }
//
// private static void copy(InputStream inputStream, OutputStream outputStream)
// throws IOException {
// byte[] buffer = new byte[8192];
// boolean finished = false;
// while (!finished) {
// int bytesRead = inputStream.read(buffer);
// if (bytesRead != -1) {
// outputStream.write(buffer, 0, bytesRead);
// } else {
// finished = true;
// }
// }
// }
//
// /**
// * The full path including the directory and file drive are only returned,
// * if the browser supports reading this properties
// *
// * @return The full file name of the last uploaded file including the file
// * path as selected by the user on his local machine.
// */
// public String getObjectPath() {
// return null;
// }
//
// public String getObjectName() {
// return fileUpload.getFileName();
// }
//
// public String getObjectType() {
// return FILE_ITEM_TYPE;
// }
//
// // protected void handleUploadFinished(final Upload upload) {
// // }
//
// /** it is caller responsability to close the stream afterwards. */
// public InputStream getFileInputStream() throws IOException {
// return new FileInputStream(file);
// // InputStream fis = null;
// //
// // try {
// // fis = new FileInputStream(file);
// // return fis;
// // } catch (Exception e) {
// // throw new ArgeoException("Unable to retrieve file " + file, e);
// // } finally {
// // IOUtils.closeQuietly(fis);
// // }
// }
//
// public boolean getNeedsProgressMonitor() {
// return false;
// }
// }
