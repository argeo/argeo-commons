include sdk.mk
include sdk/argeo-build/jpms.mk

A2_CATEGORY = org.argeo.cms

BUNDLES = \
org.argeo.init \

DEP_CATEGORIES = \
osgi/equinox/org.argeo.tp.osgi.framework \

all: osgi-all jmod-argeo-init

clean: osgi-clean
	
install: osgi-install

uninstall: osgi-uninstall

#
# PACKAGING
#

JMOD_ARGEO_INIT=org.argeo.init

jmod-argeo-init:
	$(call a2_jmod_prepare_output,$(JMOD_ARGEO_INIT))
	$(COPY) COPYING.LESSER NOTICE $(JMODS_BASE)/$(JMOD_ARGEO_INIT)/legal
	
	# config
	#mkdir -p $(JMODS_BASE)/$(JMOD_JJML)/man/examples
	#$(COPY) -v sdk/jbin/*.java $(JMODS_BASE)/$(JMOD_JJML)/man/examples

	$(call a2_jmod_create,$(JMOD_ARGEO_INIT))

include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk

.PHONY: clean all install uninstall
