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
package org.argeo.cms.e4.monitoring;

import org.argeo.cms.ui.theme.CmsImages;
import org.eclipse.swt.graphics.Image;

/** Shared icons. */
public class OsgiExplorerImages extends CmsImages {
	public final static Image INSTALLED = createIcon("installed.gif");
	public final static Image RESOLVED = createIcon("resolved.gif");
	public final static Image STARTING = createIcon("starting.gif");
	public final static Image ACTIVE = createIcon("active.gif");
	public final static Image SERVICE_PUBLISHED = createIcon("service_published.gif");
	public final static Image SERVICE_REFERENCED = createIcon("service_referenced.gif");
	public final static Image CONFIGURATION = createIcon("node.gif");
}
