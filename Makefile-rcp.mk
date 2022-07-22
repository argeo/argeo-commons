include sdk.mk
.PHONY: clean all osgi

all: osgi move-rcp

move-rcp: osgi
	mkdir -p $(A2_OUTPUT)/swt/rcp/$(A2_CATEGORY)
	mv -v $(A2_OUTPUT)/$(A2_CATEGORY)/*.rcp.$(MAJOR).$(MINOR).jar $(A2_OUTPUT)/swt/rcp/$(A2_CATEGORY)
	mv -v $(A2_OUTPUT)/$(A2_CATEGORY)/*.rcp.cli.$(MAJOR).$(MINOR).jar $(A2_OUTPUT)/swt/rcp/$(A2_CATEGORY)
	touch $(BUILD_BASE)/*.rcp/bnd.bnd

A2_CATEGORY = org.argeo.cms

BUNDLES = \
swt/rcp/org.argeo.swt.specific.rcp \
swt/rcp/org.argeo.cms.swt.rcp \
swt/rcp/org.argeo.cms.swt.rcp.cli \
swt/rcp/org.argeo.cms.e4.rcp \

A2_OUTPUT = $(SDK_BUILD_BASE)/a2
A2_BASE = $(A2_OUTPUT)

DEP_CATEGORIES = \
org.argeo.cms \
swt/org.argeo.cms \
org.argeo.tp \
org.argeo.tp.apache \
org.argeo.tp.jetty \
osgi/equinox/org.argeo.tp.eclipse \
osgi/osgi/org.argeo.tp.osgi \
swt/rcp/org.argeo.tp.swt \
lib/linux/x86_64/swt/rcp/org.argeo.tp.swt \
swt/rcp/org.argeo.tp.swt.workbench \
org.argeo.tp.jcr


clean:
	rm -rf $(BUILD_BASE)

VPATH = .:swt/rcp

include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk