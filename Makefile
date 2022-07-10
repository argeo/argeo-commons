include sdk.mk
.PHONY: clean all osgi jni

all: osgi jni move-rap
	$(MAKE) -f Makefile-rcp.mk

move-rap: osgi
	mkdir -p $(A2_OUTPUT)/$(A2_CATEGORY).eclipse.rap
	mv -v $(A2_OUTPUT)/$(A2_CATEGORY)/*.rap.$(MAJOR).$(MINOR).jar $(A2_OUTPUT)/$(A2_CATEGORY).eclipse.rap
	touch $(BUILD_BASE)/*.rap/bnd.bnd

A2_CATEGORY = org.argeo.cms

BUNDLES = \
org.argeo.init \
org.argeo.util \
org.argeo.api.uuid \
org.argeo.api.acr \
org.argeo.api.cli \
org.argeo.api.cms \
org.argeo.cms \
org.argeo.cms.sql \
org.argeo.cms.ssh \
org.argeo.cms.ux \
eclipse/org.argeo.ext.equinox.jetty \
eclipse/org.argeo.cms.servlet \
swt/org.argeo.cms.swt \
swt/org.argeo.cms.e4 \
swt/rap/org.argeo.swt.specific.rap \
swt/rap/org.argeo.cms.swt.rap \
swt/rap/org.argeo.cms.swt.rap.cli \
swt/rap/org.argeo.cms.e4.rap \
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

VPATH = .:eclipse:rap:jcr:swt:swt/rap

DEP_CATEGORIES = \
org.argeo.tp \
org.argeo.tp.apache \
org.argeo.tp.jetty \
org.argeo.tp.eclipse \
osgi/api/org.argeo.tp.osgi \
swt/rap/org.argeo.tp.swt \
swt/rap/org.argeo.tp.swt.workbench \
org.argeo.tp.jcr

jni:
	$(MAKE) -C jni

clean:
	rm -rf $(BUILD_BASE)
	$(MAKE) -C jni clean
	$(MAKE) -f Makefile-rcp.mk clean

A2_BUNDLES_CLASSPATH = $(subst $(space),$(pathsep),$(strip $(A2_BUNDLES)))

native-image:
	mkdir -p $(A2_OUTPUT)/libexec/$(A2_CATEGORY)
	cd $(A2_OUTPUT)/libexec/$(A2_CATEGORY) && /opt/graalvm-ce/bin/native-image \
		-cp $(A2_CLASSPATH):$(A2_BUNDLES_CLASSPATH) org.argeo.eclipse.ui.jetty.CmsRapCli \
		--enable-url-protocols=http,https \
		-H:AdditionalSecurityProviders=sun.security.jgss.SunProvider,org.bouncycastle.jce.provider.BouncyCastleProvider,net.i2p.crypto.eddsa.EdDSASecurityProvider \
		--initialize-at-build-time=org.argeo.init.logging.ThinLogging,org.slf4j.LoggerFactory \
		--no-fallback 


include  $(SDK_SRC_BASE)/sdk/argeo-build/osgi.mk