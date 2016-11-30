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

public class GenericUploadControl {
}
//
//
// import java.nio.file.Path;
// import java.nio.file.Paths;
//
// import org.argeo.eclipse.ui.EclipseUiUtils;
// import org.eclipse.swt.SWT;
// import org.eclipse.swt.events.ModifyListener;
// import org.eclipse.swt.layout.GridData;
// import org.eclipse.swt.layout.GridLayout;
// import org.eclipse.swt.widgets.Button;
// import org.eclipse.swt.widgets.Composite;
// import org.eclipse.swt.widgets.Event;
// import org.eclipse.swt.widgets.FileDialog;
// import org.eclipse.swt.widgets.Listener;
// import org.eclipse.swt.widgets.Text;
//
/// **
// * RAP specific composite that provides a control to upload a single file
// */
//
// public class GenericUploadControl extends Composite {
// private static final long serialVersionUID = -4079470245651908737L;
// // private final static Log log =
// // LogFactory.getLog(GenericUploadControl.class);
//
// private FileDialog dialog;
// private Text filePathTxt;
//
// public GenericUploadControl(Composite parent, int style, String browseLabel)
// {
// super(parent, style);
// createControl(this, browseLabel);
// }
//
// private void createControl(final Composite parent, String browseLabel) {
// GridLayout layout = new GridLayout(2, false);
// layout.marginHeight = layout.marginWidth = 0;
// parent.setLayout(layout);
//
// filePathTxt = new Text(parent, SWT.BORDER | SWT.SINGLE);
// filePathTxt.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL |
// GridData.FILL_HORIZONTAL));
// filePathTxt.setEditable(false);
//
// // Execute button
// Button execute = new Button(parent, SWT.PUSH);
// execute.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, false, false));
// execute.setText(browseLabel);
//
// // Button listener
// Listener executeListener = new Listener() {
// private static final long serialVersionUID = -7591211214156101065L;
//
// public void handleEvent(Event event) {
// dialog = new FileDialog(parent.getShell());
// dialog.setText("File browser");
// filePathTxt.setText(dialog.open());
// }
// };
// parent.layout();
// execute.addListener(SWT.Selection, executeListener);
// }
//
// public boolean isControlEmpty() {
// String path = filePathTxt.getText();
// if (path == null || "".equals(path.trim()))
// return true;
// else
// return false;
// }
//
// public Path getChosenFile() {
// String pathStr = filePathTxt.getText();
// if (EclipseUiUtils.isEmpty(pathStr))
// return null;
// else
// return Paths.get(filePathTxt.getText());
// }
//
// public void addModifyListener(ModifyListener listener) {
// filePathTxt.addModifyListener(listener);
// }
//
// /**
// * Always returns null in an RCP environment
// */
// public String getLastFileUploadedName() {
// return null;
// }
// }
