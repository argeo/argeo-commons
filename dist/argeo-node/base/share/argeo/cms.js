var System = Java.type("java.lang.System");
var OsgiBuilder = Java.type("org.argeo.osgi.boot.OsgiBuilder");

var osgi = new OsgiBuilder();
// default bundles
osgi.start(2, "org.eclipse.equinox.http.servlet");
osgi.start(2, "org.eclipse.equinox.http.jetty");
osgi.start(2, "org.eclipse.equinox.metatype");
osgi.start(2, "org.eclipse.equinox.cm");
osgi.start(2, "org.eclipse.equinox.ds");
osgi.start(2, "org.eclipse.rap.rwt.osgi");
osgi.start(3, "org.argeo.cms");
osgi.start(4, "org.eclipse.gemini.blueprint.extender");
osgi.start(4, "org.eclipse.equinox.http.registry");
// specific properties
osgi.conf("org.eclipse.rap.workbenchAutostart", "false");
osgi.conf("org.eclipse.equinox.http.jetty.autostart", "false");
osgi.conf("org.osgi.framework.bootdelegation", "com.sun.jndi.ldap,"
		+ "com.sun.jndi.ldap.sasl," + "com.sun.security.jgss,"
		+ "com.sun.jndi.dns," + "com.sun.nio.file," + "com.sun.nio.sctp");

var homeUri = java.nio.file.Paths
		.get(java.lang.System.getProperty("user.home")).toUri().toString();
if (typeof app !== 'undefined') {
	if (typeof appHome == 'undefined') {
		var appHome = homeUri + "/.a2/var/lib/" + app;
	}
	if (typeof appConf == 'undefined') {
		var appConf = homeUri + "/.a2/etc/" + app;
	}
	if (typeof policyFile == 'undefined') {
		var policyFile = "node.policy";
	}
	osgi.conf("osgi.configuration.area", appHome + "/state");
	osgi.conf("osgi.instance.area", appHome + "/data");
	System.setProperty("java.security.manager", "");
	System.setProperty("java.security.policy", appConf + "/" + policyFile);
	System.setProperty("log4j.configuration", appConf + "/log4j.properties");
}

function openWorkbench() {
	osgi.spring("org.argeo.cms.ui.workbench.rap");
	var appUrl = "http://127.0.0.1:" + osgi.httpPort + "/ui/node";
	$EXEC("chrome --app=" + appUrl);
	// shutdown when the window is closed
	osgi.shutdown();
}