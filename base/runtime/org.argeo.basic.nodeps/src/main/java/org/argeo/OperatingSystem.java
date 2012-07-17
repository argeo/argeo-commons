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

/** The current operating system. */
public class OperatingSystem {
	public final static int NIX = 1;
	public final static int WINDOWS = 2;
	public final static int SOLARIS = 3;

	public final static int os;
	static {
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Win"))
			os = WINDOWS;
		else if (osName.startsWith("Solaris"))
			os = SOLARIS;
		else
			os = NIX;
	}

}
