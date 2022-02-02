include sdk.mk
.PHONY: clean all osgi

all: osgi jni

BUNDLE_PREFIX = org.argeo
A2_CATEGORY = org.argeo

BUNDLES = \
org.argeo.init \
org.argeo.util \
org.argeo.api.uuid \
org.argeo.api.acr \
org.argeo.api.cms \
org.argeo.cms.tp \
org.argeo.cms \
org.argeo.cms.pgsql \
eclipse/org.argeo.cms.servlet \
rcp/org.argeo.swt.minidesktop \
rcp/org.argeo.swt.specific.rcp \
eclipse/org.argeo.cms.swt \
rcp/org.argeo.cms.ui.rcp \

NATIVE_PROJECTS = \
org.argeo.api.uuid/jni \

BUILD_CLASSPATH = \
/usr/share/java/osgi-core/osgi.core.jar:$\
/usr/share/java/osgi-compendium/osgi.cmpn.jar:$\
/usr/share/java/ecj/ecj.jar:$\
/usr/share/java/aqute-bnd/biz.aQute.bndlib.jar:$\
/usr/share/java/slf4j/api.jar:$\
/usr/share/java/commons-io.jar:$\
/usr/share/java/commons-cli.jar:$\
/usr/share/java/bcprov.jar:$\
/usr/share/java/bcpkix.jar:$\
/usr/share/java/commons-httpclient3.jar:$\
/usr/share/java/postgresql-jdbc.jar:$\
/usr/share/java/tomcat-servlet-api.jar:$\
/usr/share/java/eclipse/equinox.http.jetty.jar:$\
/usr/lib/java/swt.jar:$\
/usr/lib/eclipse/plugins/org.eclipse.swt.gtk.linux.x86_64_3.116.0.v20210304-1735:$\
/usr/lib/eclipse/plugins/org.eclipse.e4.ui.css.core_0.13.0.v20210304-1735.jar:$\
/usr/lib/eclipse/plugins/org.eclipse.e4.ui.css.swt_0.14.100.v20210304-1735.jar:$\
/usr/lib/eclipse/plugins/org.eclipse.e4.ui.css.swt.theme_0.13.0.v20210304-1735.jar:$\
/usr/lib/eclipse/plugins/org.eclipse.jface_3.22.100.v20210304-1735.jar:$\
/usr/lib/eclipse/plugins/org.eclipse.core.commands_3.9.800.v20210304-1735.jar:$\
/usr/share/java/eclipse/equinox.common.jar:$\
/usr/share/java/sac.jar:$\

JAVADOC_BUNDLES =  \
org.argeo.api.uuid \
org.argeo.api.acr \
org.argeo.api.cms

JAVADOC_PACKAGES =  \
org.argeo.api.uuid \
org.argeo.api.acr \
org.argeo.api.cms


jni:
	$(foreach dir, $(NATIVE_PROJECTS), $(MAKE) -C $(dir);)

# TODO relativize from SDK_SRC_BASE
BUILD_BASE = $(SDK_BUILD_BASE)

#
# GENERIC
#
JVM := /usr/lib/jvm/jre-11/bin/java
JAVADOC := /usr/lib/jvm/jre-11/bin/javadoc
ECJ_JAR := /usr/share/java/ecj/ecj.jar
BND_TOOL := /usr/bin/bnd

WORKSPACE_BNDS := $(shell cd $(SDK_SRC_BASE) && find cnf -name '*.bnd')
#BND_WORKSPACES := $(foreach bundle, $(BUNDLES), ./$(dir $(bundle)))
BUILD_WORKSPACE_BNDS := $(WORKSPACE_BNDS:%=$(SDK_BUILD_BASE)/%) $(WORKSPACE_BNDS:%=$(SDK_BUILD_BASE)/eclipse/%) $(WORKSPACE_BNDS:%=$(SDK_BUILD_BASE)/rcp/%)

cnf: $(BUILD_WORKSPACE_BNDS)

A2_BUNDLES = $(BUNDLES:%=$(SDK_BUILD_BASE)/a2/$(A2_CATEGORY)/%.$(MAJOR).$(MINOR).jar)

#JAVA_SRCS = $(shell find $(BUNDLE_PREFIX).* -name '*.java')
JAVA_SRCS = $(foreach bundle, $(BUNDLES), $(shell find $(bundle) -name '*.java'))
ECJ_SRCS = $(foreach bundle, $(BUNDLES), $(bundle)/src[-d $(BUILD_BASE)/$(bundle)/bin])

JAVADOC_SRCS = $(foreach bundle, $(JAVADOC_BUNDLES),$(bundle)/src)

osgi: cnf $(A2_BUNDLES)

javadoc: $(BUILD_BASE)/java-compiled
	$(JAVADOC) -d $(SDK_BUILD_BASE)/api --source-path $(subst $(space),$(pathsep),$(strip $(JAVADOC_SRCS))) -subpackages $(JAVADOC_PACKAGES)

clean:
	rm -rf $(BUILD_BASE)/*-compiled
	rm -rf $(BUILD_BASE)/{cnf,a2}
	rm -rf $(BUILD_BASE)/$(BUNDLE_PREFIX).* $(BUILD_BASE)/eclipse $(BUILD_BASE)/rcp
	$(foreach dir, $(NATIVE_PROJECTS), $(MAKE) -C $(dir) clean;)

# SDK level
$(SDK_BUILD_BASE)/cnf/%.bnd: cnf/%.bnd
	mkdir -p $(dir $@)
	cp $< $@
	
$(SDK_BUILD_BASE)/eclipse/cnf/%.bnd: cnf/%.bnd
	mkdir -p $(dir $@)
	cp $< $@

$(SDK_BUILD_BASE)/rcp/cnf/%.bnd: cnf/%.bnd
	mkdir -p $(dir $@)
	cp $< $@

$(SDK_BUILD_BASE)/a2/$(A2_CATEGORY)/%.$(MAJOR).$(MINOR).jar : $(BUILD_BASE)/%/bundle.jar
	mkdir -p $(dir $@)
	cp $< $@

# Build level
$(BUILD_BASE)/%/bundle.jar : %/bnd.bnd $(BUILD_BASE)/java-compiled 
	rsync -r --exclude "*.java" $(dir  $<)src/ $(dir $@)bin
	rsync -r $(dir  $<)src/ $(dir $@)src
	if [ -d "$(dir  $<)OSGI-INF" ]; then rsync -r $(dir  $<)OSGI-INF/ $(dir $@)/OSGI-INF; fi
	cp $< $(dir $@)
	cd $(dir $@) && $(BND_TOOL) build
	mv $(dir $@)generated/*.jar $(dir $@)bundle.jar

$(BUILD_BASE)/java-compiled : $(JAVA_SRCS)
	$(JVM) -jar $(ECJ_JAR) -11 -nowarn -time -cp $(BUILD_CLASSPATH) \
	$(ECJ_SRCS)
	touch $@
	
null  :=
space := $(null) #
pathsep := :

#WITH_LIST    := $(subst $(space),$(pathsep),$(strip $(WITH_LIST)))
	
