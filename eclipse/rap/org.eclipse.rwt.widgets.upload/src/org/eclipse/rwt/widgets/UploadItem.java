/*******************************************************************************
 * Copyright (c) 2002-2006 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Innoopract Informationssysteme GmbH - initial API and implementation
 ******************************************************************************/

package org.eclipse.rwt.widgets;

import java.io.InputStream;

/**
 * Pojo for which contains a reference to the uploaded file stream and all
 * available meta information.
 * 
 * @author Stefan.Roeck
 */
public class UploadItem {

  private final InputStream fileInputStream;
  private final String contentType;
  private final String fileName;
  private final String filePath;
  private final long fileSize;
  
  /**
   * Creates an UploadItem instance with the given parameters which all can be
   * null.
   */
  public UploadItem( final InputStream fileInputStream,
                     final String contentType,
                     final String fileName,
                     final String filePath,
                     final long fileSize)
  {
    super();
    this.fileInputStream = fileInputStream;
    this.fileName = fileName;
    this.filePath = filePath;
    this.contentType = contentType;
    this.fileSize = fileSize;
  }
  
  /**
   * Returns an inputstream to the uploaded file. Make sure to call
   * {@link InputStream#close()} if processing the file is finished.    
   */
  public InputStream getFileInputStream() {
    return fileInputStream;
  }
  
  /**
   * Returns the content type of the last uploaded file as submitted in 
   * http POST request by the browser. See RFC2616 for possible values.
   */
  public String getContentType() {
    return contentType;
  }

  
  /**
   * Returns the file name of the uploaded file without any path information.
   */
  public String getFileName() {
    return fileName;
  }
  
  /**
   * Returns the file name plus the local path where this file has been chosen
   * from. <br/> Note: In Firefox 3 the return value equals
   * {@link UploadItem#getFileName()}. (Tested with RC1)
   */
  public String getFilePath() {
    return filePath;
  }

  /**
   * Returns the size of the File in the {@link UploadItem#getFileInputStream()} thats stored in this UploadItem
   * . (Tested with RC3)
   */
  public long getFileSize() {
    return fileSize;
  }
}
