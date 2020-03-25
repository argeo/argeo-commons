import java.nio.file.*;
import org.argeo.osgi.boot.*;

OsgiBuilder osgi = new OsgiBuilder();

// default bundles
osgi.start(2, "org.eclipse.equinox.http.servlet");
osgi.start(2, "org.eclipse.equinox.metatype");
osgi.start(2, "org.eclipse.equinox.cm");
osgi.start(2, "org.eclipse.equinox.ds");
osgi.start(2, "org.eclipse.rap.rwt.osgi");
osgi.start(3, "org.argeo.cms");
osgi.start(4, "org.argeo.cms.e4.rap");

// specific properties
osgi.conf("org.eclipse.rap.workbenchAutostart", "false");
osgi.conf("org.eclipse.equinox.http.jetty.autostart", "false");
osgi.conf("org.osgi.framework.bootdelegation", "com.sun.jndi.ldap,"
		+ "com.sun.jndi.ldap.sasl," + "com.sun.security.jgss,"
		+ "com.sun.jndi.dns," + "com.sun.nio.file," + "com.sun.nio.sctp");

String homeUri = Paths.get(System.getProperty("user.home")).toUri().toString();
String execDirUri = Paths.get(System.getProperty("user.dir")).toUri().toString();

osgi.conf("osgi.configuration.area", execDirUri + "/state");
osgi.conf("osgi.instance.area", execDirUri + "/data");
System.setProperty("log4j.configuration", execDirUri + "etc/argeo/log4j.properties");
