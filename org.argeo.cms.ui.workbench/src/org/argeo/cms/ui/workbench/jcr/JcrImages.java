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
package org.argeo.cms.ui.workbench.jcr;

import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.eclipse.swt.graphics.Image;

/** Shared icons. */
public class JcrImages {
	public final static Image NODE = WorkbenchUiPlugin.getImageDescriptor(
			"icons/node.gif").createImage();
	public final static Image FOLDER = WorkbenchUiPlugin.getImageDescriptor(
			"icons/folder.gif").createImage();
	public final static Image FILE = WorkbenchUiPlugin.getImageDescriptor(
			"icons/file.gif").createImage();
	public final static Image BINARY = WorkbenchUiPlugin.getImageDescriptor(
			"icons/binary.png").createImage();
	public final static Image HOME = WorkbenchUiPlugin.getImageDescriptor(
			"icons/home.gif").createImage();
	public final static Image SORT = WorkbenchUiPlugin.getImageDescriptor(
			"icons/sort.gif").createImage();
	public final static Image REMOVE = WorkbenchUiPlugin.getImageDescriptor(
			"icons/remove.gif").createImage();

	public final static Image REPOSITORIES = WorkbenchUiPlugin
			.getImageDescriptor("icons/repositories.gif").createImage();
	public final static Image REPOSITORY_DISCONNECTED = WorkbenchUiPlugin
			.getImageDescriptor("icons/repository_disconnected.gif")
			.createImage();
	public final static Image REPOSITORY_CONNECTED = WorkbenchUiPlugin
			.getImageDescriptor("icons/repository_connected.gif").createImage();
	public final static Image REMOTE_DISCONNECTED = WorkbenchUiPlugin
			.getImageDescriptor("icons/remote_disconnected.gif").createImage();
	public final static Image REMOTE_CONNECTED = WorkbenchUiPlugin
			.getImageDescriptor("icons/remote_connected.gif").createImage();
	public final static Image WORKSPACE_DISCONNECTED = WorkbenchUiPlugin
			.getImageDescriptor("icons/workspace_disconnected.png")
			.createImage();
	public final static Image WORKSPACE_CONNECTED = WorkbenchUiPlugin
			.getImageDescriptor("icons/workspace_connected.png").createImage();

}
