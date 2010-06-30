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

package org.argeo.slc.osgiboot;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import org.argeo.slc.osgiboot.OsgiBoot;

public class OsgiBootNoRuntimeTest extends TestCase {
	public final static String BUNDLES = "src/test/bundles/some;in=*;ex=excluded,"
			+ "src/test/bundles/others;in=**/org.argeo.*";

	public void testLocations() {
		String baseUrl = "file:";
		String locations = "/mydir/myfile" + File.pathSeparator
				+ "/myotherdir/myotherfile";

		OsgiBoot osgiBoot = new OsgiBoot(null);
		List urls = osgiBoot.getLocationsUrls(baseUrl, locations);
		assertEquals(2, urls.size());
		assertEquals("file:/mydir/myfile", urls.get(0));
		assertEquals("file:/myotherdir/myotherfile", urls.get(1));
	}

	public void testBundles() {
		String baseUrl = "file:";
		String bundles = BUNDLES;
		OsgiBoot osgiBoot = new OsgiBoot(null);
		List urls = osgiBoot.getBundlesUrls(baseUrl, bundles);
		for (int i = 0; i < urls.size(); i++)
			System.out.println(urls.get(i));
		assertEquals(3, urls.size());

		List jarUrls = osgiBoot.getBundlesUrls(baseUrl,
				"src/test/bundles/jars;in=*.jar");
		for (int i = 0; i < jarUrls.size(); i++)
			System.out.println(jarUrls.get(i));
		assertEquals(1, jarUrls.size());
	}
}
