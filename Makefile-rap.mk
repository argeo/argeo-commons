include sdk.mk

A2_CATEGORY = org.argeo.cms

BUNDLES = \
swt/rap/org.argeo.swt.specific.rap \
swt/rap/org.argeo.cms.swt.rap \

DEP_CATEGORIES = \
org.argeo.cms \
swt/org.argeo.cms \
org.argeo.tp \
org.argeo.tp.httpd \
osgi/equinox/org.argeo.tp.osgi.framework \
osgi/org.argeo.tp.osgi \
swt/rap/org.argeo.tp.swt \

all: osgi-all

clean: osgi-clean
	
install: osgi-install

uninstall: osgi-uninstall

include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk

.PHONY: clean all install uninstall
