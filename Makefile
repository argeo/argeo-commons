include sdk.mk

A2_CATEGORY = org.argeo.cms

BUNDLES = \
org.argeo.api.uuid \
org.argeo.api.register \
org.argeo.api.acr \
org.argeo.api.cms \
org.argeo.cms \
org.argeo.cms.ux \
org.argeo.cms.jshell \
org.argeo.cms.lib.json \
org.argeo.cms.lib.jetty \
org.argeo.cms.lib.dbus \
org.argeo.cms.lib.sshd \
org.argeo.cms.ee \
org.argeo.cms.cli \
swt/org.argeo.swt.minidesktop \
swt/org.argeo.cms.swt \
swt/rcp/org.argeo.swt.specific.rcp \
swt/rcp/org.argeo.cms.swt.rcp \

DEP_CATEGORIES = \
crypto/fips/org.argeo.tp.crypto \
org.argeo.tp \
org.argeo.tp.httpd \
org.argeo.tp.sshd \
osgi/equinox/org.argeo.tp.osgi.framework \
osgi/org.argeo.tp.osgi \
swt/rcp/org.argeo.tp.swt \
lib/local/swt/rcp/org.argeo.tp.swt \

JAVADOC_PACKAGES =  \
org.argeo.api.uuid \
org.argeo.api.acr \
org.argeo.api.cms

all: osgi-all
	$(MAKE) -f Makefile-init.mk all
	$(MAKE) -f Makefile-rap.mk all
	
clean: osgi-clean
	$(MAKE) -f Makefile-init.mk clean
	$(MAKE) -f Makefile-rap.mk clean

install: osgi-install
	$(MAKE) -f Makefile-init.mk install
	$(MAKE) -f Makefile-rap.mk install

uninstall: osgi-uninstall
	$(MAKE) -f Makefile-init.mk uninstall
	$(MAKE) -f Makefile-rap.mk uninstall

include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk

.PHONY: clean all install uninstall
