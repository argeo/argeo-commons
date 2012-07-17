/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo;

/** Argeo Commons specific exception. */
public class ArgeoException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/** Creates an exception with a message. */
	public ArgeoException(String message) {
		super(message);
	}

	/** Creates an exception with a message and a root cause. */
	public ArgeoException(String message, Throwable e) {
		super(message, e);
	}

	/** @deprecated use {@link #ArgeoException(String, Throwable)} instead. */
	@Deprecated
	public ArgeoException(Throwable cause) {
		super(cause);
	}

}
