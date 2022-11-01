include sdk.mk
.PHONY: clean all osgi

all: osgi
	$(MAKE) -f Makefile-rcp.mk all

A2_CATEGORY = org.argeo.cms

BUNDLES = \
org.argeo.init \
org.argeo.util \
org.argeo.api.uuid \
org.argeo.api.acr \
org.argeo.api.cli \
org.argeo.api.cms \
org.argeo.cms \
org.argeo.cms.ux \
org.argeo.cms.ee \
org.argeo.cms.lib.jetty \
org.argeo.cms.lib.equinox \
org.argeo.cms.lib.sshd \
org.argeo.cms.lib.pgsql \
org.argeo.cms.cli \
swt/org.argeo.swt.minidesktop \
swt/org.argeo.cms.swt \
swt/org.argeo.cms.e4 \
swt/rap/org.argeo.swt.specific.rap \
swt/rap/org.argeo.cms.swt.rap \
swt/rap/org.argeo.cms.e4.rap \

DEP_CATEGORIES = \
org.argeo.tp \
org.argeo.tp.apache \
org.argeo.tp.jetty \
osgi/api/org.argeo.tp.osgi \
osgi/equinox/org.argeo.tp.eclipse \
swt/rap/org.argeo.tp.swt \
swt/rap/org.argeo.tp.swt.workbench \
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