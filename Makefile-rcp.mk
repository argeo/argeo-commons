include sdk.mk
.PHONY: clean all osgi

all: osgi

A2_CATEGORY = org.argeo.cms.eclipse.rcp

BUNDLES = \
rcp/org.argeo.swt.minidesktop \
swt/rcp/org.argeo.swt.specific.rcp \
swt/rcp/org.argeo.cms.swt.rcp \
swt/rcp/org.argeo.cms.swt.rcp.cli \
swt/rcp/org.argeo.cms.e4.rcp \

A2_OUTPUT = $(SDK_BUILD_BASE)/a2
A2_BASE = $(A2_OUTPUT)

DEP_CATEGORIES = \
org.argeo.cms \
org.argeo.tp \
org.argeo.tp.apache \
org.argeo.tp.jetty \
org.argeo.tp.eclipse \
osgi/osgi/org.argeo.tp.osgi \
swt/rcp/org.argeo.tp.swt \
lib/linux/x86_64/swt/rcp/org.argeo.tp.swt \
swt/rcp/org.argeo.tp.swt.workbench \
org.argeo.tp.jcr


clean:
	rm -rf $(BUILD_BASE)

VPATH = .:rcp:swt/rcp

include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk