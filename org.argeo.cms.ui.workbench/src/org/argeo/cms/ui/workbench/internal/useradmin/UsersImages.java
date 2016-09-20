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
package org.argeo.cms.ui.workbench.internal.useradmin;

import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.eclipse.swt.graphics.Image;

/** Specific users icons. TODO centralize and use an image registry */
public class UsersImages {
	private final static String PREFIX = "icons/";

	public final static Image ICON_USER = WorkbenchUiPlugin.getImageDescriptor(
			PREFIX + "user.gif").createImage();
	public final static Image ICON_GROUP = WorkbenchUiPlugin.getImageDescriptor(
			PREFIX + "users.gif").createImage();
	public final static Image ICON_ROLE = WorkbenchUiPlugin.getImageDescriptor(
			PREFIX + "role.gif").createImage();
}
