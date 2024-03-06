include sdk.mk

A2_CATEGORY = org.argeo.cms

BUNDLES = \
swt/rcp/org.argeo.swt.specific.rcp \
swt/rcp/org.argeo.cms.swt.rcp \

DEP_CATEGORIES = \
org.argeo.cms \
swt/org.argeo.cms \
org.argeo.tp \
org.argeo.tp.httpd \
osgi/equinox/org.argeo.tp.eclipse \
osgi/api/org.argeo.tp.osgi \
swt/rcp/org.argeo.tp.swt \
lib/linux/x86_64/swt/rcp/org.argeo.tp.swt \

VPATH = .:swt/rcp

all: osgi-all

clean: osgi-clean
	
install: osgi-install

uninstall: osgi-uninstall

include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk

.PHONY: clean all install uninstall
