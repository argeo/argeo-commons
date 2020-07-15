package org.argeo.osgi.boot;

import java.util.List;

import junit.framework.TestCase;

/** Tests which do not require a runtime. */
@SuppressWarnings("rawtypes")
public class OsgiBootNoRuntimeTest extends TestCase {
	public final static String BUNDLES = "src/test/bundles/some;in=*;ex=excluded,"
			+ "src/test/bundles/others;in=**/org.argeo.*";

	/** Tests that location lists are properly parsed. */
	// public void testLocations() {
	// String baseUrl = "file:";
	// String locations = "/mydir/myfile" + File.pathSeparator
	// + "/myotherdir/myotherfile";
	//
	// OsgiBoot osgiBoot = new OsgiBoot(null);
	// osgiBoot.setExcludeSvn(true);
	// List urls = osgiBoot.getLocationsUrls(baseUrl, locations);
	// assertEquals(2, urls.size());
	// assertEquals("file:/mydir/myfile", urls.get(0));
	// assertEquals("file:/myotherdir/myotherfile", urls.get(1));
	// }

	/** Tests that bundle lists are properly parsed. */
	public void testBundles() {
		String baseUrl = "file:";
		String bundles = BUNDLES;
		OsgiBoot osgiBoot = new OsgiBoot(null);
//		osgiBoot.setExcludeSvn(true);
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
