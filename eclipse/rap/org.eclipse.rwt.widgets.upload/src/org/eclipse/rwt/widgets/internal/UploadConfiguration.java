/*******************************************************************************
 * Copyright (c) 2002-2006 Innoopract Informationssysteme GmbH. All rights
 * reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors: Innoopract Informationssysteme GmbH - initial API and
 * implementation
 ******************************************************************************/
package org.eclipse.rwt.widgets.internal;

import org.eclipse.rwt.widgets.IUploadConfiguration;

public class UploadConfiguration implements IUploadConfiguration {

  private long fileSizeMax = -1;
  private long sizeMax = -1;

  public synchronized long getFileSizeMax() {
    return fileSizeMax;
  }

  public synchronized long getSizeMax() {
    return sizeMax;
  }

  public synchronized void setFileMaxSize( final long fileSizeMax ) {
    this.fileSizeMax = fileSizeMax;
  }

  public synchronized void setSizeMax( final long sizeMax ) {
    this.sizeMax = sizeMax;
  }
}
