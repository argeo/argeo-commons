# Argeo Commons lightweight integration framework

Based on the OSGi and JCR standards. Enterprise-grade since 2009.

**The reference for Argeo code is http://git.argeo.org/ ** and this GitHub repository is just a read-only mirror.

## Development

### Pre-requisite
- Java 11 OpenJDK (https://adoptopenjdk.net/)
- Maven 3 (https://maven.apache.org/download.cgi)
- Eclipse 2019-12 for RCP and RAP Developers (https://www.eclipse.org/downloads/packages/release/2019-12/r/eclipse-ide-rcp-and-rap-developers-includes-incubating-components)

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

### Release
```bash
mvn release:prepare
mvn release:perform -Prpmbuild-tp,rpmbuild
createrepo /srv/rpmfactory/argeo-osgi-2/el7/
```

## Deployment

### Pre-requisite
- CentOS/RHEL 7 or 8

### Install
Please refer to the appropriate documentation for more complex deployments (e.g. LDAP server, PostgreSQL backend, etc.).

```bash
sudo yum install java-11-openjdk-headless
sudo yum install argeo-node argeo-cms-e4-rap
# Required only for local access to the OSGi console (telnet localhost 2323):
sudo yum install telnet
```

Firewall
```bash
sudo firewall-cmd --add-port 8080/tcp
```

### Running with systemd
```bash
systemctl [start|stop|restart] argeo
# Make sure that the service is started after a server restart:
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