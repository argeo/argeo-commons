include sdk.mk
.PHONY: clean all osgi

all: osgi

A2_CATEGORY = org.argeo.cms.eclipse.rcp

BUNDLES = \
rcp/org.argeo.cms.e4.rcp \
rcp/org.argeo.cms.ui.rcp \
rcp/org.argeo.swt.minidesktop \
rcp/org.argeo.swt.specific.rcp \

A2_OUTPUT = $(SDK_BUILD_BASE)/a2
A2_BASE = $(A2_OUTPUT)

DEP_CATEGORIES = \
org.argeo.cms \
org.argeo.tp \
org.argeo.tp.apache \
org.argeo.tp.jetty \
org.argeo.tp.eclipse.equinox \
org.argeo.tp.eclipse.rcp \
org.argeo.tp.jcr


clean:
	rm -rf $(BUILD_BASE)

VPATH = .:rcp

include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk