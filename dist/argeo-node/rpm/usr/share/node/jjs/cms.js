var System = Java.type("java.lang.System")
var OsgiBuilder = Java.type("org.argeo.osgi.boot.OsgiBuilder");

var osgi = new OsgiBuilder();
osgi.start(2, "org.eclipse.equinox.http.servlet");
osgi.start(2, "org.eclipse.equinox.http.jetty");
osgi.start(2, "org.eclipse.equinox.metatype");
osgi.start(2, "org.eclipse.equinox.cm");
osgi.start(2, "org.eclipse.rap.rwt.osgi");
osgi.start(3, "org.argeo.cms");
osgi.start(4, "org.eclipse.gemini.blueprint.extender");
osgi.start(4, "org.eclipse.equinox.http.registry");
osgi.conf("org.eclipse.rap.workbenchAutostart", "false");
osgi.conf("org.eclipse.equinox.http.jetty.autostart", "false");
osgi.conf("org.osgi.framework.bootdelegation", "com.sun.jndi.ldap,"
		+ "com.sun.jndi.ldap.sasl," + "com.sun.security.jgss,"
		+ "com.sun.jndi.dns," + "com.sun.nio.file," + "com.sun.nio.sctp");

if (typeof app !== 'undefined') {
	if (typeof appHome == 'undefined') {
		var appHome = $ENV.HOME + "/.a2/var/lib/" + app;
	}
	if (typeof appConf == 'undefined') {
		var appConf = $ENV.HOME + "/.a2/etc/" + app;
	}
	if (typeof policyFile == 'undefined') {
		var policyFile = app + ".policy";
	}
	osgi.conf("osgi.configuration.area", appHome + "/state");
	osgi.conf("osgi.instance.area", appHome + "/data");
	System.setProperty("java.security.manager", "");
	System.setProperty("java.security.policy", "file://" + appConf + "/"
			+ policyFile);
	System.setProperty("log4j.configuration", "file://" + appConf
			+ "/log4j.properties");
}

function openUi(){
	osgi.spring("org.argeo.cms.ui.workbench.rap");
	var appUrl = "http://localhost:" + osgi.httpPort + "/ui/node";
	$EXEC("/usr/bin/chromium-browser --app=" + appUrl);
	// shutdown when the window is closed
	osgi.shutdown();
}