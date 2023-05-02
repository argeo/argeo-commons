include sdk.mk
.PHONY: clean all osgi

all: osgi
	$(MAKE) -f Makefile-rcp.mk all

A2_CATEGORY = org.argeo.cms

BUNDLES = \
org.argeo.init \
org.argeo.api.uuid \
org.argeo.api.register \
org.argeo.api.acr \
org.argeo.api.cli \
org.argeo.api.cms \
org.argeo.cms \
org.argeo.cms.ux \
org.argeo.cms.ee \
org.argeo.cms.lib.jetty \
org.argeo.cms.lib.dbus \
org.argeo.cms.lib.sshd \
org.argeo.cms.jshell \
org.argeo.cms.cli \
osgi/equinox/org.argeo.cms.lib.equinox \
swt/org.argeo.swt.minidesktop \
swt/org.argeo.cms.swt \
swt/rap/org.argeo.swt.specific.rap \
swt/rap/org.argeo.cms.swt.rap \

DEP_CATEGORIES = \
crypto/fips/org.argeo.tp.crypto \
org.argeo.tp \
org.argeo.tp.httpd \
osgi/api/org.argeo.tp.osgi \
osgi/equinox/org.argeo.tp.eclipse \
swt/rap/org.argeo.tp.swt \
$(A2_CATEGORY) \
swt/$(A2_CATEGORY) \
swt/rap/$(A2_CATEGORY) \

JAVADOC_PACKAGES =  \
org.argeo.api.uuid \
org.argeo.api.acr \
org.argeo.api.cms

clean:
	rm -rf $(BUILD_BASE)
	$(MAKE) -f Makefile-rcp.mk clean

A2_BUNDLES_CLASSPATH = $(subst $(space),$(pathsep),$(strip $(A2_BUNDLES)))

include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk