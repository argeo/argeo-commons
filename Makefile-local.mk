include sdk.mk

# Build using a plain classpath (no A2) and locally deployed artifacts.
# Typically used for standard distribution packaging.

export ARGEO_BUILD_IGNORE_A2 = true

# Debian
export ARGEO_MAKE_CLASSPATH ?= $\
/usr/share/java/slf4j-api.jar$\
:/usr/share/java/eclipse-jdt-core-compiler-batch.jar$\
:/usr/share/java/bndlib.jar

export ARGEO_BUILD_CLASSPATH_EXTRA ?= $\
/usr/share/java/eclipse-osgi.jar$\
:/usr/share/java/org.eclipse.swt.jar$\
:/usr/share/java/osgi.compendium.jar$\
:/usr/share/java/sshd-core.jar$\
:/usr/share/java/sshd-scp.jar$\
:/usr/share/java/sshd-common.jar$\
:/usr/share/java/sshd-sftp.jar$\
:/usr/share/java/bcpg.jar$\
:/usr/share/java/bcpkix.jar$\
:/usr/share/java/bcprov.jar$\
:/usr/share/java/bcutil.jar$\
:/usr/share/java/jakarta-activation.jar$\
:/usr/share/java/jakarta-mail.jar$\
:/usr/share/java/jakarta-mail-dsn.jar$\
:/usr/share/java/jakarta-mail-gimap.jar$\
:/usr/share/java/jakarta-mail-mailapi.jar$\
:/usr/share/java/jakarta-servlet-api.jar$\
:/usr/share/java/jetty12-client.jar$\
:/usr/share/java/jetty12-ee.jar$\
:/usr/share/java/jetty12-http.jar$\
:/usr/share/java/jetty12-io.jar$\
:/usr/share/java/jetty12-server.jar$\
:/usr/share/java/jetty12-util.jar$\
:/usr/share/java/servlet-api.jar$\
:/usr/share/java/dbus.jar$\
:/usr/share/java/jetty12-alpn-server.jar$\
:/usr/share/java/jetty12-http2-client.jar$\
:/usr/share/java/jetty12-http2-common.jar$\
:/usr/share/java/jetty12-http2-server.jar$\
:/usr/share/java/jetty12-alpn-java-server.jar$\
:/usr/share/java/jetty12-http2-hpack.jar$\
:/usr/share/java/jetty12-session.jar$\
:/usr/share/java/jetty12-ee10-websocket-jakarta-client.jar$\
:/usr/share/java/jetty12-ee10-websocket-jakarta-common.jar$\
:/usr/share/java/jetty12-ee10-websocket-jakarta-server.jar$\
:/usr/share/java/jetty12-ee10-servlet.jar$\
:/usr/share/java/jetty12-ee10-websocket-jetty-server.jar$\
:/usr/share/java/jetty12-ee10-annotations.jar$\
:/usr/share/java/jetty12-websocket-core-client.jar$\
:/usr/share/java/jetty12-websocket-core-common.jar$\
:/usr/share/java/jetty12-websocket-core-server.jar$\
:/usr/share/java/jetty12-websocket-jetty-api.jar$\
:/usr/share/java/jetty12-websocket-jetty-client.jar$\
:/usr/share/java/jetty12-websocket-jetty-common.jar$\
:/usr/share/java/jetty12-websocket-jetty-server.jar$\
:/usr/share/java/eclipse-jface.jar$\
:/usr/share/java/eclipse-core-commands.jar$\
:/usr/share/java/eclipse-e4-ui-css-core.jar$\
:/usr/share/java/eclipse-e4-ui-css-swt.jar$\
:/usr/share/java/sac.jar$\
:/usr/share/java/jakarta.json-api.jar$\


A2_CATEGORY = org.argeo.cms

BUNDLES = \
org.argeo.init \
org.argeo.api.uuid \
org.argeo.api.register \
org.argeo.api.acr \
org.argeo.api.cms \
org.argeo.cms \
org.argeo.cms.ux \
org.argeo.cms.jshell \
org.argeo.cms.lib.jetty \
org.argeo.cms.lib.sshd \
org.argeo.cms.lib.dbus \
org.argeo.cms.lib.mail \
org.argeo.cms.lib.json \
org.argeo.cms.ee \
org.argeo.cms.cli \
swt/org.argeo.swt.minidesktop \
swt/org.argeo.cms.swt \
swt/rcp/org.argeo.swt.specific.rcp \
swt/rcp/org.argeo.cms.swt.rcp \

all: osgi-all
	
clean: osgi-clean

install: osgi-install

uninstall: osgi-uninstall

include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk

.PHONY: clean all install uninstall
