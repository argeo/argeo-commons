include sdk.mk
.PHONY: clean all osgi jni

all: osgi jni move-rap
	$(MAKE) -f Makefile-rcp.mk

move-rap:
	mkdir -p $(A2_OUTPUT)/$(A2_CATEGORY).eclipse.rap
	mv -v $(A2_OUTPUT)/$(A2_CATEGORY)/*.rap.$(MAJOR).$(MINOR).jar $(A2_OUTPUT)/$(A2_CATEGORY).eclipse.rap
	touch $(BUILD_BASE)/*.rap/bnd.bnd

A2_CATEGORY = org.argeo.cms

BUNDLES = \
org.argeo.init \
org.argeo.util \
org.argeo.api.uuid \
org.argeo.api.acr \
org.argeo.api.cms \
org.argeo.cms \
org.argeo.cms.pgsql \
eclipse/org.argeo.cms.servlet \
eclipse/org.argeo.cms.swt \
eclipse/org.argeo.cms.e4 \
rap/org.argeo.cms.ui.rap \
rap/org.argeo.swt.specific.rap \
rap/org.argeo.cms.e4.rap \
jcr/org.argeo.cms.jcr \
jcr/org.argeo.cms.ui \

JAVADOC_BUNDLES =  \
org.argeo.api.uuid \
org.argeo.api.acr \
org.argeo.api.cms

JAVADOC_PACKAGES =  \
org.argeo.api.uuid \
org.argeo.api.acr \
org.argeo.api.cms

A2_OUTPUT = $(SDK_BUILD_BASE)/a2
A2_BASE = $(A2_OUTPUT)

VPATH = .:eclipse:rap:jcr

DEP_CATEGORIES = \
org.argeo.tp \
org.argeo.tp.apache \
org.argeo.tp.jetty \
org.argeo.tp.eclipse.equinox \
org.argeo.tp.eclipse.rwt \
org.argeo.tp.eclipse.rap \
org.argeo.tp.jcr

jni:
	$(MAKE) -C jni

clean:
	rm -rf $(BUILD_BASE)
	$(MAKE) -C jni clean
	$(MAKE) -f Makefile-rcp.mk clean

include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk