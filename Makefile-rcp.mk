include sdk.mk
.PHONY: clean all osgi

all: osgi 

A2_CATEGORY = org.argeo.cms

BUNDLES = \
swt/rcp/org.argeo.swt.specific.rcp \
swt/rcp/org.argeo.cms.swt.rcp \
swt/rcp/org.argeo.cms.e4.rcp \

DEP_CATEGORIES = \
org.argeo.cms \
swt/org.argeo.cms \
org.argeo.tp \
org.argeo.tp.crypto \
org.argeo.tp.jetty \
osgi/equinox/org.argeo.tp.eclipse \
osgi/api/org.argeo.tp.osgi \
swt/rcp/org.argeo.tp.swt \
lib/linux/x86_64/swt/rcp/org.argeo.tp.swt \
swt/rcp/org.argeo.tp.swt.workbench \


clean:
	rm -rf $(BUILD_BASE)

VPATH = .:swt/rcp

include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk