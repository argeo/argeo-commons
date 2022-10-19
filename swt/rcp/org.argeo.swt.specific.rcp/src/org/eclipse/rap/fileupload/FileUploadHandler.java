/*******************************************************************************
 * Copyright (c) 2011, 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.rap.fileupload;

/**
 * A file upload handler is used to accept file uploads from a client. After
 * creating a file upload handler, the server will accept file uploads to the
 * URL returned by <code>getUploadUrl()</code>. Upload listeners can be attached
 * to react on progress. When the upload has finished, a FileUploadHandler has
 * to be disposed of by calling its <code>dispose()</code> method.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class FileUploadHandler {

	public FileUploadHandler(FileUploadReceiver fileUploadReceiver) {
	}

	public void dispose() {

	}

	public void addUploadListener(FileUploadListener listener) {

	}

	public String getUploadUrl() {
		return null;
	}
}
