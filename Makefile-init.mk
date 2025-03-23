include sdk.mk

A2_CATEGORY = org.argeo.cms

BUNDLES = \
org.argeo.init \

DEP_CATEGORIES = \
osgi/equinox/org.argeo.tp.osgi.framework \

all: osgi-all

clean: osgi-clean
	
install: osgi-install

uninstall: osgi-uninstall

include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk

.PHONY: clean all install uninstall
