/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
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

package org.argeo.security.core;

import org.argeo.security.ArgeoSecurityService;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

public class SystemAuthenticatedTaskExecutor extends SimpleAsyncTaskExecutor {
	private static final long serialVersionUID = 453384889461147359L;

	private ArgeoSecurityService securityService;

	@Override
	public Thread createThread(Runnable runnable) {
		return super.createThread(securityService
				.wrapWithSystemAuthentication(runnable));
	}

	public void setSecurityService(ArgeoSecurityService securityService) {
		this.securityService = securityService;
	}

}
