# Argeo Commons lightweight integration framework

Based on the OSGi and JCR standards. Enterprise-grade since 2009.

**The reference for Argeo code is http://git.argeo.org/ ** (the GitHub mirrors are updated from time to time, usually after releases)

## Development

### Pre-requisite
- Java 8 OpenJDK (with HotSpot or OpenJ9 JVM https://adoptopenjdk.net/)
- Maven 3 (https://maven.apache.org/download.cgi)
- Eclipse 2019-06 for RCP and RAP Developers (https://www.eclipse.org/downloads/packages/release/2019-06/r/eclipse-ide-rcp-and-rap-developers)

### First build, or after dependencies update
```bash
mvn clean install argeo-osgi:pde-sources
```

This will download the sources of all third-party Java libraries in a format compatible with the Eclipse IDE, and will generate the Eclipse PDE target platform (Select ```org.argeo.dep.cms.e4.rap``` in Window > Preferences > Plug-in Development > Target Platform).

### While developing
```bash
mvn clean install
```

Fully functional SNAPSHOT RPMs can be built with (make sure your user has write access to /srv/rpmfactory):
```bash
mvn clean install -Prpmbuild-tp,rpmbuild
createrepo /srv/rpmfactory/argeo-osgi-2/el7/
```

### Usage in Eclipse
The project can be run as a standard PDE OSGi runtime, but the Argeo SLC Eclipse plug-in greatly simplifies the configuration of an Eclipse PDE running configuration:
- Install the Argeo SLC plug-in from this Eclipse update site https://projects.argeo.org/slc/update/ide/
- In the demo/ directory, right-click on cms-e4-rap.properties, ```Run As > OSGi Boot (Equinox, RAP)```
- The execution directory will be sdk/exec/cms-e4-rap
- The standard OSGi data area will be sdk/exec/cms-e4-rap/data (contains mostly the Jackrabbit JCR repository)
- Open http://localhost:7070/cms/devops with the username ```root``` and the password ```demo```

### Release
```bash
mvn release:prepare
mvn release:perform -Prpmbuild-tp,rpmbuild
createrepo /srv/rpmfactory/argeo-osgi-2/el7/
```

## Deployment

### Pre-requisite
- CentOS 7

### Install
Please refer to the appropriate documentation for more complex deployments (e.g. LDAP server, PostgreSQL backend, etc.).

```bash
sudo yum install java-1.8.0-openjdk-headless
sudo yum install argeo-node argeo-cms-e4-rap
# required only for local access to the OSGi console (telnet localhost 2323):
sudo yum install telnet
```

Firewall
```bash
sudo firewall-cmd --add-port 8080/tcp
```

### Running with systemd
```bash
systemctl [start|stop|restart] argeo
# make sure that it started after a server restart:
systemctl enable argeo
```

The system is accessible via HTTP port 8080:
http://localhost:8080/cms/devops

HTTP protection is configured on the reverse HTTP proxy. For example on Apache v2.4:

```
<IfModule mod_ssl.c>
<VirtualHost <external IP address>:443>
  ServerName <external hostname>
  CustomLog logs/<external hostname>-access_log combined
  ErrorLog logs/<external hostname>-error_log

  ProxyPreserveHost On

  <Location / >
    ProxyPass http://<host on the local network>:8080/
    ProxyPassReverse http://<host on the local network>:8080/
  </Location>


SSLCertificateFile <path to certificates>/cert.pem
SSLCertificateKeyFile <path to certificates>/privkey.pem
SSLCertificateChainFile <path to certificates>/chain.pem
...

</VirtualHost>
</IfModule>
```

### Log
```bash
sudo tail -200f /var/log/messages
```